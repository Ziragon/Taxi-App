import json
import os
import time
import threading
import requests
import websocket
import random
from dotenv import load_dotenv, find_dotenv

load_dotenv(find_dotenv())

API_URL = os.getenv("TAXI_API_URL", "http://localhost:8000/api/v1")
WS_URL  = os.getenv("TAXI_WS_URL",  "ws://localhost:8083/ws/notifications/websocket")
STRIPE_API_KEY = os.getenv("STRIPE_API_KEY")
DEFAULT_STATE_FILE = "state.json"


def _print_json(data: dict):
    """Вывод JSON в консоль."""
    print(json.dumps(data, ensure_ascii=False, indent=2))


class PassengerService:
    def __init__(self, state_file=DEFAULT_STATE_FILE, email_override=None):
        self.state_file = state_file
        self.email_override = email_override
        self.full_state = None
        self.state = self.load_state()
        self.token = None
        self.current_trip_id = None
        self.ws = None
        self.ws_thread = None
        self.on_ws_message = None

    def load_state(self):
        if not os.path.exists(self.state_file):
            raise FileNotFoundError(f"Файл состояния не найден: {self.state_file}")
        with open(self.state_file, "r") as f:
            data = json.load(f)

        self.full_state = data

        if isinstance(data, dict) and "passengers" in data and len(data["passengers"]) > 0:
            passengers = data["passengers"]
            if self.email_override:
                profile = next((p for p in passengers if p["passenger_email"] == self.email_override), None)
                if not profile:
                    raise ValueError(f"Пассажир {self.email_override} не найден в state.json")
                return profile
            print("[*] Email не указан, выбираю случайного пассажира...")
            return random.choice(passengers)
        raise ValueError("В state.json нет зарегистрированных пассажиров.")

    def save_token(self, token):
        self.state["passenger_token"] = token
        if isinstance(self.full_state, dict) and "passengers" in self.full_state:
            for p in self.full_state["passengers"]:
                if p.get("passenger_email") == self.state.get("passenger_email"):
                    p["passenger_token"] = token
                    break
            with open(self.state_file, "w") as f:
                json.dump(self.full_state, f, indent=4)

    @property
    def email(self):
        return self.state["passenger_email"]

    def _auth_headers(self):
        return {"Authorization": f"Bearer {self.token}", "Content-Type": "application/json"}

    def login(self):
        print(f"[*] Логин пассажира: {self.email}")
        payload = {
            "email": self.state["passenger_email"],
            "password": self.state["passenger_password"],
        }
        try:
            res = requests.post(f"{API_URL}/auth/login", json=payload, timeout=10)
            if res.status_code == 200:
                self.token = res.json()["accessTokenDto"]["token"]
                self.save_token(self.token)
                print(f"[+] Авторизован: {self.email}")
                return True
            print(f"[!] Ошибка логина ({res.status_code}): {res.text}")
        except Exception as e:
            print(f"[!] Auth-Service недоступен: {e}")
        return False

    def _stomp_frame(self, command, headers, body=""):
        frame = f"{command}\n"
        for k, v in headers.items():
            frame += f"{k}:{v}\n"
        frame += f"\n{body}\x00"
        return frame

    def _heartbeat_loop(self):
        while self.ws and self.ws.keep_running:
            try:
                self.ws.send("\n")
            except Exception:
                break
            time.sleep(10)

    def connect_ws(self, block=False):
        if not self.token:
            print("[!] Нет токена для WS. Сначала нужно авторизоваться.")
            return

        ws_url = f"{WS_URL}?token={self.token}"

        def on_open(ws):
            print(f"[+] WS открыт ({self.email}). STOMP CONNECT...")
            ws.send(self._stomp_frame("CONNECT", {
                "accept-version": "1.1",
                "heart-beat": "10000,10000",
                "host": "localhost",
                "userType": "PASSENGER",
            }))

        def on_message(ws, message):
            if message == "\n":
                return

            if message.startswith("CONNECTED"):
                print(f"[✔] STOMP подключён ({self.email}). Подписка на уведомления...")
                ws.send(self._stomp_frame("SUBSCRIBE", {
                    "id": "sub-0",
                    "destination": "/user/queue/notifications"
                }))
                threading.Thread(target=self._heartbeat_loop, daemon=True).start()

            elif message.startswith("MESSAGE"):
                print(f"\n[🔔 {self.email} WS]:\n{message}\n")
                if self.on_ws_message:
                    self.on_ws_message(message)

        def on_error(ws, err):
            print(f"[!] WS ошибка ({self.email}): {err}")

        def on_close(ws, *args):
            print(f"[-] WS закрыт ({self.email})")

        self.ws = websocket.WebSocketApp(
            ws_url,
            on_open=on_open,
            on_message=on_message,
            on_error=on_error,
            on_close=on_close,
        )

        if block:
            self.ws.run_forever()
        else:
            self.ws_thread = threading.Thread(target=self.ws.run_forever, daemon=True)
            self.ws_thread.start()

    def disconnect_ws(self):
        if self.ws:
            self.ws.close()

    def attach_card(self, set_as_default=False):
        if not STRIPE_API_KEY:
            print("[!] STRIPE_API_KEY не установлен. Экспортни его в консоли.")
            return False

        print("[*] Создаем тестовую карту (tok_visa) в Stripe...")
        stripe_url = "https://api.stripe.com/v1/payment_methods"
        headers = {
            "Authorization": f"Bearer {STRIPE_API_KEY}",
            "Content-Type": "application/x-www-form-urlencoded"
        }
        data = "type=card&card[token]=tok_visa"

        try:
            res = requests.post(stripe_url, headers=headers, data=data)
            if res.status_code != 200:
                print(f"[!] Ошибка Stripe API ({res.status_code}): {res.text}")
                return False

            pm_id = res.json()["id"]
            print(f"[+] PaymentMethod создан в Stripe: {pm_id}")

            backend_url = f"{API_URL}/payment-methods"
            payload = {
                "stripePaymentMethodId": pm_id,
                "setAsDefault": set_as_default
            }

            print(f"[*] Привязываем карту к аккаунту пассажира...")
            b_res = requests.post(backend_url, headers=self._auth_headers(), json=payload)

            if b_res.status_code in [200, 201]:
                data = b_res.json()
                print(f"[✔] Карта привязана: {data.get('cardBrand', 'card')} **** {data.get('lastFour', '****')}")
                return True
            elif b_res.status_code == 409:
                print(f"[·] Карта уже привязана (409 Conflict)")
                return True
            else:
                print(f"[!] Ошибка бэкенда ({b_res.status_code}): {b_res.text}")
                return False

        except Exception as e:
            print(f"[!] Ошибка HTTP: {e}")
            return False

    def create_trip(self, origin_addr, origin_lat, origin_lng, dest_addr, dest_lat, dest_lng):
        """POST /api/v1/trips — создаёт черновик поездки и возвращает тарифы."""
        url = f"{API_URL}/trips"
        payload = {
            "originAddress": origin_addr,
            "originLat": origin_lat,
            "originLng": origin_lng,
            "destAddress": dest_addr,
            "destLat": dest_lat,
            "destLng": dest_lng
        }

        print(f"[*] Запрос на создание поездки: {origin_addr} -> {dest_addr}")
        try:
            res = requests.post(url, headers=self._auth_headers(), json=payload)
            if res.status_code in [200, 201]:
                data = res.json()
                self.current_trip_id = data.get("id")
                print(f"[+] Поездка #{self.current_trip_id} создана.")
                print(f"    Дистанция: {data.get('distanceKm')} км, Время: {data.get('durationMin')} мин")
                print("  --- JSON ответ ---")
                _print_json(data)
                return True
            else:
                print(f"[!] Ошибка создания поездки ({res.status_code}): {res.text}")
                return False
        except Exception as e:
            print(f"[!] Ошибка соединения: {e}")
            return False

    def start_search(self, vehicle_class="COMFORT"):
        """POST /api/v1/trips/start-search — выбор тарифа и запуск поиска водителя."""
        url = f"{API_URL}/trips/start-search?vehicleClass={vehicle_class}"
        print(f"[*] Запуск поиска водителя (класс: {vehicle_class})...")

        try:
            res = requests.post(url, headers=self._auth_headers(), json={})

            if res.status_code in [200, 201]:
                data = res.json()
                self.current_trip_id = data.get("id")
                print(f"[✔] Поиск запущен. Поездка #{self.current_trip_id}")
                print("  --- JSON ответ ---")
                _print_json(data)
                return True
            else:
                print(f"[!] Ошибка запуска поиска ({res.status_code}): {res.text}")
                return False
        except Exception as e:
            print(f"[!] Ошибка соединения: {e}")
            return False

    def cancel_trip(self, trip_id):
        """POST /api/v1/trips/{id}/cancel — отмена поездки по явно переданному id."""
        url = f"{API_URL}/trips/{trip_id}/cancel"
        print(f"[*] Отмена поездки #{trip_id}...")

        try:
            res = requests.post(url, headers=self._auth_headers(), json={})
            if res.status_code in [200, 204]:
                print(f"[✔] Поездка #{trip_id} отменена.")
                if self.current_trip_id == int(trip_id):
                    self.current_trip_id = None
                return True
            else:
                print(f"[!] Ошибка отмены поездки ({res.status_code}): {res.text}")
                return False
        except Exception as e:
            print(f"[!] Ошибка соединения: {e}")
            return False

    def get_active_trip(self):
        """GET /api/v1/trips/active — возвращает активную поездку пассажира (при перезаходе)."""
        url = f"{API_URL}/trips/active"
        print("[*] Запрос активной поездки...")

        try:
            res = requests.get(url, headers=self._auth_headers())
            if res.status_code == 200:
                data = res.json()
                self.current_trip_id = data.get("id")
                print(f"[✔] Активная поездка #{self.current_trip_id}, статус: {data.get('status')}")
                print("  --- JSON ответ ---")
                _print_json(data)
                return data
            elif res.status_code == 204:
                print("[·] Активной поездки нет.")
                self.current_trip_id = None
                return None
            else:
                print(f"[!] Ошибка запроса активной поездки ({res.status_code}): {res.text}")
                return None
        except Exception as e:
            print(f"[!] Ошибка соединения: {e}")
            return None
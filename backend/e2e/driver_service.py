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

class DriverService:
    def __init__(self, state_file=DEFAULT_STATE_FILE, lat=55.755864, lng=37.617617, email_override=None):
        self.state_file     = state_file
        self.email_override = email_override
        self.full_state     = None
        self.state          = self.load_state()
        self.start_lat      = lat
        self.start_lng      = lng
        self.token          = None
        self.ws             = None
        self.ws_thread      = None
        self.on_ws_message = None

    # ------------------------------------------------------------------ state

    def load_state(self):
        if not os.path.exists(self.state_file):
            raise FileNotFoundError(f"Файл состояния не найден: {self.state_file}")
        with open(self.state_file, "r") as f:
            data = json.load(f)

        self.full_state = data          # ← запоминаем полный объект

        if isinstance(data, dict) and "drivers" in data:
            drivers = data["drivers"]
            if self.email_override:
                profile = next((d for d in drivers if d["driver_email"] == self.email_override), None)
                if not profile:
                    raise ValueError(f"Водитель {self.email_override} не найден в state.json")
                return profile
            print("[*] Email не указан, выбираю случайного водителя...")
            return random.choice(drivers)
        return data

    def save_token(self, token):
        self.state["driver_token"] = token

        if isinstance(self.full_state, dict) and "drivers" in self.full_state:
            for d in self.full_state["drivers"]:
                if d.get("driver_email") == self.state.get("driver_email"):
                    d["driver_token"] = token
                    break
            with open(self.state_file, "w") as f:
                json.dump(self.full_state, f, indent=4)
        else:
            with open(self.state_file, "w") as f:
                json.dump(self.state, f, indent=4)

    @property
    def email(self):
        return self.state["driver_email"]

    # ------------------------------------------------------------------- auth

    def login(self):
        """Логин, сохраняет токен в self.token. Возвращает True/False."""
        print(f"[*] Логин: {self.email}")
        payload = {
            "email":    self.state["driver_email"],
            "password": self.state["driver_password"],
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

    # ----------------------------------------------------------------- status

    def _auth_headers(self):
        return {"Authorization": f"Bearer {self.token}", "accept": "*/*"}

    def go_online(self):
        """PUT /api/v1/driver/online"""
        try:
            res = requests.put(f"{API_URL}/driver/online", headers=self._auth_headers(), timeout=10)
            if res.status_code in [200, 204]:
                print(f"[✔] {self.email} → ONLINE")
                return True
            print(f"[!] go_online ошибка ({res.status_code}): {res.text}")
        except Exception as e:
            print(f"[!] HTTP ошибка: {e}")
        return False

    def go_offline(self):
        """PUT /api/v1/driver/offline"""
        try:
            res = requests.put(f"{API_URL}/driver/offline", headers=self._auth_headers(), timeout=10)
            if res.status_code in [200, 204]:
                print(f"[✔] {self.email} → OFFLINE")
                return True
            print(f"[!] go_offline ошибка ({res.status_code}): {res.text}")
        except Exception as e:
            print(f"[!] HTTP ошибка: {e}")
        return False

    def get_status(self):
        """GET /api/v1/driver/status → ONLINE / OFFLINE / BUSY или None при ошибке"""
        try:
            res = requests.get(f"{API_URL}/driver/status", headers=self._auth_headers(), timeout=10)
            if res.status_code == 200:
                return res.json().get("status", "UNKNOWN")
            print(f"[!] get_status ошибка ({res.status_code}): {res.text}")
        except Exception as e:
            print(f"[!] HTTP ошибка: {e}")
        return None

    # --------------------------------------------------------------- websocket

    def _stomp_frame(self, command, headers, body=""):
        frame = f"{command}\n"
        for k, v in headers.items():
            frame += f"{k}:{v}\n"
        frame += f"\n{body}\x00"
        return frame

    def _location_loop(self):
        payload = {
            "longitude":    self.start_lng,
            "latitude":     self.start_lat,
            "vehicleClass": self.state.get("vehicle_class", "COMFORT"),
        }
        while self.ws and self.ws.keep_running:
            try:
                frame = self._stomp_frame(
                    "SEND",
                    {"destination": "/app/driver/location", "content-type": "application/json"},
                    json.dumps(payload),
                )
                self.ws.send(frame)
                print(f"[📍 {self.email}] {payload['latitude']:.6f}, {payload['longitude']:.6f}")
            except Exception as e:
                print(f"[!] Ошибка отправки локации: {e}")
            time.sleep(5)

    def connect_ws(self, block=False):
        """
        Подключает WebSocket + STOMP.
        block=False — WS в фоновом daemon-потоке (UI, E2E).
        block=True  — блокирует текущий поток (standalone демон).
        """
        ws_url = f"{WS_URL}?token={self.token}"

        def on_open(ws):
            print(f"[+] WS открыт ({self.email}). STOMP CONNECT...")
            ws.send(self._stomp_frame("CONNECT", {
                "accept-version": "1.1",
                "heart-beat": "10000,10000",
                "host": "localhost",
                "userType": "DRIVER",
            }))

        def on_message(ws, message):
            if message.startswith("CONNECTED"):
                print(f"[✔] STOMP подключён ({self.email}). Подписка...")
                ws.send(self._stomp_frame("SUBSCRIBE", {"id": "sub-0", "destination": "/user/queue/notifications"}))
                threading.Thread(target=self._location_loop, daemon=True).start()
            elif message.startswith("MESSAGE"):
                print(f"\n[🔔 {self.email}]:\n{message}\n")
                if self.on_ws_message:
                    self.on_ws_message(message)

        self.ws = websocket.WebSocketApp(
            ws_url,
            on_open=on_open,
            on_message=on_message,
            on_error=lambda ws, err: print(f"[!] WS ошибка ({self.email}): {err}"),
            on_close=lambda ws, *a: print(f"[-] WS закрыт ({self.email})"),
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
        """Создает тестовый PaymentMethod в Stripe и привязывает к водителю"""
        if not STRIPE_API_KEY:
            print("[!] STRIPE_API_KEY не установлен.")
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
                print(f"[!] Ошибка Stripe API: {res.text}")
                return False

            pm_id = res.json()["id"]
            print(f"[+] PaymentMethod создан: {pm_id}")

            # Исправлено: рут и payload как в test-payment.html
            backend_url = f"{API_URL}/payment-methods"
            payload = {
                "stripePaymentMethodId": pm_id,
                "setAsDefault": set_as_default
            }

            b_res = requests.post(backend_url, headers=self._auth_headers(), json=payload)

            if b_res.status_code in [200, 201]:
                data = b_res.json()
                print(f"[✔] Карта привязана: {data.get('cardBrand', 'card')} **** {data.get('lastFour', '****')}")
                return True
            else:
                print(f"[!] Ошибка бэкенда ({b_res.status_code}): {b_res.text}")
                return False

        except Exception as e:
            print(f"[!] Ошибка: {e}")
            return False

    def attach_payout_account(self) -> bool:
        """Привязать банковский счёт для выплат водителю"""
        if not self.token:
            return False

        try:
            check_res = requests.get(
                f"{API_URL}/payout-accounts",
                headers={"Authorization": f"Bearer {self.token}"},
                timeout=10,
            )

            if check_res.status_code == 200:
                accounts = check_res.json()
                if accounts:
                    print(f"[·] Payout account уже существует — пропускаем")
                    return True
        except Exception:
            pass

        driver_id = self.state.get("driver_id")
        payload = {
            "stripeAccountId": f"acct_fake_{driver_id}",
            "lastFour": "1234"
        }

        try:
            res = requests.post(
                f"{API_URL}/payout-accounts",
                headers={
                    "Authorization": f"Bearer {self.token}",
                    "Content-Type": "application/json"
                },
                json=payload,
                timeout=10,
            )

            if res.status_code in [200, 201]:
                data = res.json()
                print(f"[✔] Payout account привязан: **** {data.get('lastFour', '1234')}")
                return True

            if res.status_code == 409:
                print(f"[·] Payout account уже существует (конфликт при создании)")
                return True

            print(f"[!] Не удалось привязать payout account: {res.status_code} {res.text}")
            return False

        except Exception as e:
            print(f"[!] Ошибка HTTP: {e}")
            return False

    def accept_trip(self, trip_id):
        """POST /api/v1/trips/{id}/accept - Принятие поездки с отправкой локации"""
        url = f"{API_URL}/trips/{trip_id}/accept"

        # Теперь отправляем текущие координаты водителя в Body
        payload = {
            "latitude": self.start_lat,
            "longitude": self.start_lng
        }

        print(f"[*] Принятие поездки #{trip_id} (Локация: {self.start_lat}, {self.start_lng})...")

        try:
            res = requests.post(url, headers=self._auth_headers(), json=payload)
            if res.status_code == 200:
                data = res.json()
                print(f"[✔] Поездка #{trip_id} принята!")
                print(f"    Маршрут до пассажира: {data.get('distanceKm')} км, ~{data.get('durationMin')} мин")
                print(f"    String: {data.get('geometry')}")
                return data
            elif res.status_code == 204:
                print(f"[✔] Поездка #{trip_id} принята (без данных о маршруте).")
                return True
            else:
                print(f"[!] Ошибка принятия поездки ({res.status_code}): {res.text}")
                return False
        except Exception as e:
            print(f"[!] Ошибка соединения: {e}")
            return False

    def reject_trip(self, trip_id):
        """POST /api/v1/trips/{id}/reject - Отклонение поездки водителем"""
        url = f"{API_URL}/trips/{trip_id}/reject"
        print(f"[*] Отклонение поездки #{trip_id}...")

        try:
            res = requests.post(url, headers=self._auth_headers(), json={})
            if res.status_code in [200, 204]:
                print(f"[✔] Поездка #{trip_id} отклонена.")
                return True
            else:
                print(f"[!] Ошибка отклонения поездки ({res.status_code}): {res.text}")
                return False
        except Exception as e:
            print(f"[!] Ошибка соединения: {e}")
            return False

    def start_trip(self, trip_id):
        """POST /api/v1/trips/{id}/start - Начало поездки (пассажир в машине)"""
        url = f"{API_URL}/trips/{trip_id}/start"
        print(f"[*] Старт поездки #{trip_id}...")

        try:
            res = requests.post(url, headers=self._auth_headers(), json={})
            if res.status_code in [200, 204]:
                print(f"[✔] Поездка #{trip_id} начата. Вы в пути!")
                return True
            else:
                print(f"[!] Ошибка старта поездки ({res.status_code}): {res.text}")
                return False
        except Exception as e:
            print(f"[!] Ошибка соединения: {e}")
            return False

    def complete_trip(self, trip_id):
        """POST /api/v1/trips/{id}/complete - Завершение поездки"""
        url = f"{API_URL}/trips/{trip_id}/complete"
        print(f"[*] Завершение поездки #{trip_id}...")

        try:
            res = requests.post(url, headers=self._auth_headers(), json={})
            if res.status_code in [200, 204]:
                print(f"[✔] Поездка #{trip_id} успешно завершена!")
                return True
            else:
                print(f"[!] Ошибка завершения поездки ({res.status_code}): {res.text}")
                return False
        except Exception as e:
            print(f"[!] Ошибка соединения: {e}")
            return False
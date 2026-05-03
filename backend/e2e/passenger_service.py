import json
import os
import requests
import random
from dotenv import load_dotenv, find_dotenv

load_dotenv(find_dotenv())

API_URL = os.getenv("TAXI_API_URL", "http://localhost:8000/api/v1")
STRIPE_API_KEY = os.getenv("STRIPE_API_KEY")
DEFAULT_STATE_FILE = "state.json"

class PassengerService:
    def __init__(self, state_file=DEFAULT_STATE_FILE, email_override=None):
        self.state_file = state_file
        self.email_override = email_override
        self.full_state = None
        self.state = self.load_state()
        self.token = None

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

    def attach_card(self, set_as_default=False):
        """Создает тестовый PaymentMethod в Stripe и отправляет его на наш бэкенд"""
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
                # Карта уже привязана
                print(f"[·] Карта уже привязана (409 Conflict)")
                return True
            else:
                print(f"[!] Ошибка бэкенда ({b_res.status_code}): {b_res.text}")
                return False

        except Exception as e:
            print(f"[!] Ошибка HTTP: {e}")
            return False

    def create_trip(self, origin_addr, origin_lat, origin_lng, dest_addr, dest_lat, dest_lng):
        """POST /api/v1/trips - Создает предварительный расчет поездки"""
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
                print(f"[+] Поездка #{self.current_trip_id} успешно создана.")
                print(f"    Дистанция: {data.get('distanceKm')} км, Время: {data.get('durationMin')} мин")
                return True
            else:
                print(f"[!] Ошибка создания поездки ({res.status_code}): {res.text}")
                return False
        except Exception as e:
            print(f"[!] Ошибка соединения: {e}")
            return False

    def start_search(self, vehicle_class="COMFORT"):
        """POST /api/v1/trips/{id}/start-search - Запуск поиска водителя"""
        if not self.current_trip_id:
            print("[!] Сначала нужно создать поездку (команда: create_trip).")
            return False

        url = f"{API_URL}/trips/{self.current_trip_id}/start-search?vehicleClass={vehicle_class}"
        print(f"[*] Запуск поиска водителя (класс: {vehicle_class}) для поездки #{self.current_trip_id}...")

        try:
            # Отправляем пустой json или data, т.к. бэкенд ожидает POST
            res = requests.post(url, headers=self._auth_headers(), json={})
            if res.status_code == 204:
                print("[✔] Поиск успешно запущен (в фоновом режиме).")
                return True
            else:
                print(f"[!] Ошибка запуска поиска ({res.status_code}): {res.text}")
                return False
        except Exception as e:
            print(f"[!] Ошибка соединения: {e}")
            return False

    def cancel_trip(self):
        """POST /api/v1/trips/{id}/cancel - Отмена поездки"""
        if not self.current_trip_id:
            print("[!] Нет активной поездки для отмены.")
            return False

        url = f"{API_URL}/trips/{self.current_trip_id}/cancel"
        print(f"[*] Отмена поездки #{self.current_trip_id}...")

        try:
            res = requests.post(url, headers=self._auth_headers(), json={})
            # Ожидаем 200 или 204 в зависимости от реализации бэкенда
            if res.status_code in [200, 204]:
                print("[✔] Поездка отменена.")
                self.current_trip_id = None
                return True
            else:
                print(f"[!] Ошибка отмены поездки ({res.status_code}): {res.text}")
                return False
        except Exception as e:
            print(f"[!] Ошибка соединения: {e}")
            return False
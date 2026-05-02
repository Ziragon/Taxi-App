import json
import os
import time
import threading
import requests
import websocket
import random

API_URL = os.getenv("TAXI_API_URL", "http://localhost:8000/api/v1")
WS_URL  = os.getenv("TAXI_WS_URL",  "ws://localhost:8083/ws/notifications/websocket")
DEFAULT_STATE_FILE = "state.json"


class DriverService:
    def __init__(self, state_file=DEFAULT_STATE_FILE, lat=55.755864, lng=37.617617, email_override=None):
        self.state_file     = state_file
        self.email_override = email_override
        self.state          = self.load_state()
        self.start_lat      = lat
        self.start_lng      = lng
        self.token          = None
        self.ws             = None
        self.ws_thread      = None

        # Переопределяй снаружи для обработки входящих WS-сообщений:
        #   service.on_ws_message = lambda msg: ...
        self.on_ws_message = None

    # ------------------------------------------------------------------ state

    def load_state(self):
        if not os.path.exists(self.state_file):
            raise FileNotFoundError(f"Файл состояния не найден: {self.state_file}")
        with open(self.state_file, "r") as f:
            data = json.load(f)
        if isinstance(data, list):
            if self.email_override:
                profile = next((d for d in data if d["driver_email"] == self.email_override), None)
                if not profile:
                    raise ValueError(f"Водитель {self.email_override} не найден в state.json")
                return profile
            print("[*] Email не указан, выбираю случайного водителя...")
            return random.choice(data)
        return data

    def save_token(self, token):
        self.state["driver_token"] = token
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
                payload["latitude"] += 0.00001
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
            ws.send(self._stomp_frame("CONNECT", {"accept-version": "1.1", "heart-beat": "10000,10000"}))

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
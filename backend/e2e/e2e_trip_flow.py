"""
Что делает:
  1. Регистрирует пассажира (или берёт существующего из passenger_state.json)
  2. Загружает случайного водителя из state.json
  3. Водитель логинится, подключается по WS, уходит в ONLINE
  4. Пассажир создаёт поездку
  5. Пассажир запускает поиск
  6. Водитель получает уведомление по WS и автоматически принимает оффер
  7. Водитель стартует поездку
  8. Водитель завершает поездку
  9. Итоговый отчёт

Запуск:
    python e2e_trip_flow.py
    python e2e_trip_flow.py --driver-email driver_abc@example.com
    python e2e_trip_flow.py --no-register   # взять пассажира из passenger_state.json
"""

import argparse
import json
import os
import random
import re
import threading
import time
import redis
import uuid
from dataclasses import dataclass, field
from typing import Optional

import requests

from driver_service import DriverService

# ---------------------------------------------------------------------------
# Конфигурация
# ---------------------------------------------------------------------------

API_URL            = os.getenv("TAXI_API_URL", "http://localhost:8000/api/v1")
STATE_FILE         = "state.json"
PASSENGER_STATE    = "passenger_state.json"
REDIS_URL          = os.getenv("TAXI_REDIS_URL", "redis://localhost:6379")

# Координаты — Москва, центр
ORIGIN_LAT, ORIGIN_LNG   = 55.755864, 37.617617
DEST_LAT,   DEST_LNG     = 55.730000, 37.650000

ORIGIN_ADDRESS = "Красная площадь, Москва"
DEST_ADDRESS   = "Парк Горького, Москва"

VEHICLE_CLASS  = "COMFORT"

# Сколько секунд ждём уведомления от WS после start-search
WS_OFFER_TIMEOUT = 40

# ---------------------------------------------------------------------------
# Утилиты
# ---------------------------------------------------------------------------


def check_redis_status(driver_id: int):
    """Проверить статус водителя в Redis"""
    try:
        r = redis.Redis.from_url(REDIS_URL, decode_responses=True)
        status_key = f"driver:status:{driver_id}"
        status = r.get(status_key)
        ttl = r.ttl(status_key)
        info(f"Redis driver:status:{driver_id} = {status} (TTL: {ttl}s)")
        return status
    except Exception as e:
        info(f"Redis check failed: {e}")
        return None


def step(n: int, text: str):
    print(f"\n{'='*60}")
    print(f"  Шаг {n}: {text}")
    print(f"{'='*60}")


def ok(text: str):
    print(f"  [✔] {text}")


def fail(text: str):
    print(f"  [✗] {text}")
    raise SystemExit(1)


def info(text: str):
    print(f"  [·] {text}")


# ---------------------------------------------------------------------------
# Пассажир
# ---------------------------------------------------------------------------

@dataclass
class PassengerSession:
    email: str
    password: str
    token: str
    account_id: int

    def headers(self):
        return {"Authorization": f"Bearer {self.token}", "Content-Type": "application/json"}


def load_passenger() -> Optional[PassengerSession]:
    """Загрузить пассажира из passenger_state.json"""
    if not os.path.exists(PASSENGER_STATE):
        return None
    with open(PASSENGER_STATE) as f:
        data = json.load(f)
    token_res = requests.post(
        f"{API_URL}/auth/login",
        json={"email": data["email"], "password": data["password"]},
        timeout=10,
    )
    if token_res.status_code != 200:
        return None
    token = token_res.json()["accessTokenDto"]["token"]
    return PassengerSession(
        email=data["email"],
        password=data["password"],
        token=token,
        account_id=data["account_id"],
    )


def register_passenger() -> PassengerSession:
    """Зарегистрировать нового пассажира и создать профиль"""
    email    = f"passenger_{uuid.uuid4().hex[:8]}@example.com"
    phone    = f"+7{random.randint(9000000000, 9999999999)}"
    password = "SecurePass123"

    info(f"Регистрация пассажира: {email}")

    res = requests.post(
        f"{API_URL}/auth/register",
        json={"email": email, "phone": phone, "password": password},
        timeout=10,
    )
    if res.status_code not in [200, 201]:
        fail(f"Регистрация пассажира: {res.status_code} {res.text}")

    data       = res.json()
    account_id = data["id"]
    token      = data["accessTokenDto"]["token"]

    headers = {"Authorization": f"Bearer {token}", "Content-Type": "application/json"}

    # Создаём пассажирский профиль
    profile_res = requests.post(
        f"{API_URL}/profiles/passenger",
        headers=headers,
        json={
            "firstName": "Тест",
            "lastName":  "Пассажир",
            "photoUrl":  "https://cdn.example.com/passenger.jpg",
        },
        timeout=10,
    )
    if profile_res.status_code not in [200, 201]:
        fail(f"Создание профиля пассажира: {profile_res.status_code} {profile_res.text}")

    # Сохраняем
    with open(PASSENGER_STATE, "w") as f:
        json.dump({"email": email, "password": password, "account_id": account_id}, f, indent=4)
    ok(f"Пассажир зарегистрирован: {email} (id={account_id})")

    return PassengerSession(email=email, password=password, token=token, account_id=account_id)


# ---------------------------------------------------------------------------
# Создание и запуск поездки
# ---------------------------------------------------------------------------

def create_trip(passenger: PassengerSession) -> dict:
    payload = {
        "originAddress": ORIGIN_ADDRESS,
        "originLat":     ORIGIN_LAT,
        "originLng":     ORIGIN_LNG,
        "destAddress":   DEST_ADDRESS,
        "destLat":       DEST_LAT,
        "destLng":       DEST_LNG,
    }
    res = requests.post(f"{API_URL}/trips", headers=passenger.headers(), json=payload, timeout=30)
    if res.status_code not in [200, 201]:
        fail(f"Создание поездки: {res.status_code} {res.text}")
    data = res.json()
    ok(f"Поездка создана: id={data['id']}, статус={data.get('status')}")
    return data


def start_search(passenger: PassengerSession, trip_id: int) -> None:
    res = requests.post(
        f"{API_URL}/trips/{trip_id}/start-search",
        headers=passenger.headers(),
        params={"vehicleClass": VEHICLE_CLASS},
        timeout=15,
    )
    if res.status_code not in [200, 204]:
        fail(f"start-search: {res.status_code} {res.text}")
    ok(f"Поиск водителя запущен для trip={trip_id}")


# ---------------------------------------------------------------------------
# Водитель — HTTP действия
# ---------------------------------------------------------------------------

def driver_accept_trip(driver: DriverService, trip_id: int) -> bool:
    res = requests.post(
        f"{API_URL}/trips/{trip_id}/accept",
        headers={"Authorization": f"Bearer {driver.token}"},
        timeout=10,
    )
    if res.status_code in [200, 204]:
        ok(f"Водитель {driver.email} принял поездку {trip_id}")
        return True
    info(f"accept вернул {res.status_code}: {res.text}")
    return False


def driver_start_trip(driver: DriverService, trip_id: int) -> bool:
    res = requests.post(
        f"{API_URL}/trips/{trip_id}/start",
        headers={"Authorization": f"Bearer {driver.token}"},
        timeout=10,
    )
    if res.status_code in [200, 204]:
        ok(f"Водитель стартовал поездку {trip_id}")
        return True
    info(f"start вернул {res.status_code}: {res.text}")
    return False


def driver_complete_trip(driver: DriverService, trip_id: int) -> bool:
    res = requests.post(
        f"{API_URL}/trips/{trip_id}/complete",
        headers={"Authorization": f"Bearer {driver.token}"},
        timeout=10,
    )
    if res.status_code in [200, 204]:
        ok(f"Водитель завершил поездку {trip_id}")
        return True
    info(f"complete вернул {res.status_code}: {res.text}")
    return False


# ---------------------------------------------------------------------------
# Парсинг WS-уведомления
# ---------------------------------------------------------------------------

def extract_trip_id_from_ws(message: str) -> Optional[int]:
    """
    Ищем tripId в STOMP MESSAGE body.
    Body — JSON вида {"notificationId":...,"tripId":123,...}
    """
    # Тело STOMP-фрейма идёт после двойного \n
    body_match = re.search(r"\n\n(.+)\x00?$", message, re.DOTALL)
    if not body_match:
        return None
    try:
        payload = json.loads(body_match.group(1).strip())
        trip_id = payload.get("tripId")
        return int(trip_id) if trip_id is not None else None
    except (json.JSONDecodeError, ValueError):
        return None


def extract_event_type(message: str) -> Optional[str]:
    """Извлечь eventType из STOMP MESSAGE"""
    parts = message.split('\n\n', 1)
    if len(parts) < 2:
        return None

    body = parts[1].rstrip('\x00')
    try:
        payload = json.loads(body)
        event_type = payload.get("eventType")
        return str(event_type) if event_type else None
    except (json.JSONDecodeError, ValueError) as e:
        print(f"[DEBUG] Failed to parse eventType: {e}")
        return None


# ---------------------------------------------------------------------------
# Основной флоу
# ---------------------------------------------------------------------------

def run(args):
    print("\n" + "█" * 60)
    print("  E2E TRIP FLOW — TaxiApp")
    print("█" * 60)

    # ------------------------------------------------------------------
    # Шаг 1: Пассажир
    # ------------------------------------------------------------------
    step(1, "Подготовка пассажира")

    if args.no_register:
        passenger = load_passenger()
        if passenger is None:
            fail(f"passenger_state.json не найден или логин не прошёл. Убери --no-register")
        ok(f"Загружен существующий пассажир: {passenger.email}")
    else:
        passenger = register_passenger()

    # ------------------------------------------------------------------
    # Шаг 2: Водитель
    # ------------------------------------------------------------------
    step(2, "Подготовка водителя")

    driver = DriverService(
        state_file=STATE_FILE,
        lat=ORIGIN_LAT,
        lng=ORIGIN_LNG,
        email_override=args.driver_email,
    )

    if not driver.login():
        fail("Водитель не смог авторизоваться")
    ok(f"Водитель авторизован: {driver.email}")

    # ------------------------------------------------------------------
    # Шаг 3: WS-подключение водителя
    # ------------------------------------------------------------------
    step(3, "WebSocket подключение водителя")

    # Событие — сигнал что пришёл оффер
    offer_event     = threading.Event()
    received_trip_id: list[Optional[int]] = [None]   # list для мутации из closure

    def on_driver_message(message: str):
        event_type = extract_event_type(message)
        info(f"WS водителя: eventType={event_type}")

        if event_type == "TRIP_OFFER":
            trip_id = extract_trip_id_from_ws(message)
            info(f"Получен оффер: tripId={trip_id}")
            received_trip_id[0] = trip_id
            offer_event.set()

    driver.on_ws_message = on_driver_message
    driver.connect_ws(block=False)

    time.sleep(2)  # ждём STOMP CONNECTED + SUBSCRIBE
    ok("WS водителя подключён")

    # ------------------------------------------------------------------
    # Шаг 4: Водитель → ONLINE
    # ------------------------------------------------------------------
    step(4, "Водитель переходит в ONLINE")

    if not driver.go_online():
        fail("Не удалось перевести водителя в ONLINE")
    ok("Водитель ONLINE")

    # ВАЖНО: Локация отправляется каждые 5с через WS, ждём первой отправки
    info("Ожидание обновления локации в Redis (10с)...")
    time.sleep(10)

    # Логи
    redis_status = check_redis_status(driver.state.get('driver_id'))
    if redis_status is None:
        fail(f"Статус водителя НЕ в Redis после go_online! Проверь user-service.")
    if redis_status != "ONLINE":
        fail(f"Статус в Redis = {redis_status}, ожидалось ONLINE")
    ok(f"Redis статус: {redis_status}")

    # ------------------------------------------------------------------
    # Шаг 5: Пассажир создаёт поездку
    # ------------------------------------------------------------------
    step(5, "Пассажир создаёт поездку")

    trip_data = create_trip(passenger)
    trip_id   = trip_data["id"]

    # Если сервер вернул тарифы — показываем
    tariffs = trip_data.get("tariffs", [])
    if tariffs:
        info(f"Доступные тарифы:")
        for t in tariffs:
            info(f"  {t.get('tripClass')} — {t.get('prices', {}).get('price')} $ "
                 f"({t.get('driversNearby', 0)} водителей рядом)")

    # ------------------------------------------------------------------
    # Шаг 6: Пассажир запускает поиск
    # ------------------------------------------------------------------
    step(6, "Пассажир запускает поиск водителя")

    start_search(passenger, trip_id)

    # ------------------------------------------------------------------
    # Шаг 7: Ждём оффер на WS водителя
    # ------------------------------------------------------------------
    step(7, f"Ожидание оффера водителю (до {WS_OFFER_TIMEOUT}с)")

    offer_arrived = offer_event.wait(timeout=WS_OFFER_TIMEOUT)

    if not offer_arrived:
        fail(
            f"Водитель не получил оффер за {WS_OFFER_TIMEOUT}с. "
            "Проверь: водитель ONLINE, координаты в радиусе поиска, "
            "notification-service запущен, RabbitMQ работает."
        )

    ws_trip_id = received_trip_id[0]
    if ws_trip_id is not None and ws_trip_id != trip_id:
        info(f"WARN: tripId в уведомлении ({ws_trip_id}) ≠ созданному ({trip_id}), используем {trip_id}")

    ok(f"Оффер получен! tripId={trip_id}")


    # ------------------------------------------------------------------
    # Шаг 7.5: Водитель автоматически принимает оффер
    # ------------------------------------------------------------------
    step(7.5, "Водитель автоматически принимает оффер")

    info(f"Отправка Accept для trip={trip_id}...")
    redis_status_before = check_redis_status(driver.state.get('driver_id'))
    if redis_status_before != "ONLINE":
        fail(f"Статус перед Accept = {redis_status_before}, ожидается ONLINE")

    time.sleep(1)

    if not driver_accept_trip(driver, trip_id):
        fail("Водитель не смог принять поездку")

    redis_status_after = check_redis_status(driver.state.get('driver_id'))
    info(f"Статус после Accept = {redis_status_after}")

    time.sleep(2)

    # ------------------------------------------------------------------
    # Шаг 8: Водитель стартует поездку
    # ------------------------------------------------------------------
    step(8, "Водитель стартует поездку")

    if not driver_start_trip(driver, trip_id):
        fail("Не удалось стартовать поездку")

    time.sleep(2)

    # ------------------------------------------------------------------
    # Шаг 9: Водитель завершает поездку
    # ------------------------------------------------------------------
    step(9, "Водитель завершает поездку")

    if not driver_complete_trip(driver, trip_id):
        fail("Не удалось завершить поездку")



    # ------------------------------------------------------------------
    # Итог
    # ------------------------------------------------------------------
    print("\n" + "█" * 60)
    print("  E2E ЗАВЕРШЁН УСПЕШНО ✔")
    print(f"  trip_id    : {trip_id}")
    print(f"  пассажир   : {passenger.email}")
    print(f"  водитель   : {driver.email}")
    print("█" * 60 + "\n")

    driver.disconnect_ws()


# ---------------------------------------------------------------------------
# CLI
# ---------------------------------------------------------------------------

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="E2E Trip Flow")
    parser.add_argument(
        "--driver-email", type=str, default=None,
        help="Email конкретного водителя из state.json (по умолчанию — случайный)"
    )
    parser.add_argument(
        "--no-register", action="store_true",
        help="Не регистрировать пассажира, взять из passenger_state.json"
    )
    args = parser.parse_args()
    run(args)
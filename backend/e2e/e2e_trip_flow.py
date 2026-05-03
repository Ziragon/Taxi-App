"""
e2e_trip_flow.py — полный E2E сценарий поездки

Что делает:
  1. Загружает пассажира из state.json (или регистрирует нового)
  2. Привязывает карту пассажиру через Stripe
  3. Загружает случайного водителя из state.json
  4. Привязывает payout account водителю (если --no-payout не указан)
  5. Водитель логинится, подключается по WS, уходит в ONLINE
  6. Пассажир создаёт поездку
  7. Пассажир запускает поиск
  8. Водитель получает уведомление по WS и автоматически принимает оффер
  9. Водитель стартует поездку
  10. Водитель завершает поездку
  11. Ожидание подтверждения оплаты (COMPLETED)
  12. Итоговый отчёт

Запуск:
    python e2e_trip_flow.py (если зарегистрированы driver и passenger)
    python e2e_trip_flow.py --driver-email driver_abc@example.com
    python e2e_trip_flow.py --passenger-email passenger_abc@example.com
    python e2e_trip_flow.py --no-card
    python e2e_trip_flow.py --no-payout
"""

import argparse
import json
import os
import threading
import time
import redis as redis_lib
from typing import Optional

import requests
from dotenv import load_dotenv, find_dotenv

from driver_service import DriverService, DEFAULT_STATE_FILE
from passenger_service import PassengerService

load_dotenv(find_dotenv())

# ---------------------------------------------------------------------------
# Конфигурация
# ---------------------------------------------------------------------------

API_URL        = os.getenv("TAXI_API_URL", "http://localhost:8000/api/v1")
REDIS_URL      = os.getenv("TAXI_REDIS_URL", "redis://localhost:6379")
STATE_FILE     = DEFAULT_STATE_FILE

# Координаты — Москва, центр
ORIGIN_LAT, ORIGIN_LNG = 55.755864, 37.617617
DEST_LAT,   DEST_LNG   = 55.730000, 37.650000

ORIGIN_ADDRESS = "Красная площадь, Москва"
DEST_ADDRESS   = "Парк Горького, Москва"

VEHICLE_CLASS    = "COMFORT"
WS_OFFER_TIMEOUT = 40  # секунд ждём оффер

# ---------------------------------------------------------------------------
# Утилиты вывода
# ---------------------------------------------------------------------------

def step(n, text: str):
    print(f"\n{'=' * 60}")
    print(f"  Шаг {n}: {text}")
    print(f"{'=' * 60}")

def ok(text: str):   print(f"  [✔] {text}")
def fail(text: str):
    print(f"  [✗] {text}")
    raise SystemExit(1)
def info(text: str): print(f"  [·] {text}")

# ---------------------------------------------------------------------------
# Redis утилита
# ---------------------------------------------------------------------------

def check_redis_driver_status(driver_id: int) -> Optional[str]:
    try:
        r = redis_lib.Redis.from_url(REDIS_URL, decode_responses=True)
        key = f"driver:status:{driver_id}"
        status = r.get(key)
        ttl = r.ttl(key)
        info(f"Redis {key} = {status} (TTL: {ttl}s)")
        return status
    except Exception as e:
        info(f"Redis check failed: {e}")
        return None

# ---------------------------------------------------------------------------
# Парсинг WS-сообщений
# ---------------------------------------------------------------------------

def extract_body(message: str) -> Optional[dict]:
    """Извлечь JSON тело из STOMP MESSAGE фрейма"""
    parts = message.split('\n\n', 1)
    if len(parts) < 2:
        return None
    body = parts[1].rstrip('\x00').strip()
    try:
        return json.loads(body)
    except (json.JSONDecodeError, ValueError):
        return None

def extract_event_type(message: str) -> Optional[str]:
    payload = extract_body(message)
    if not payload:
        return None
    return payload.get("eventType")

def extract_trip_id(message: str) -> Optional[int]:
    payload = extract_body(message)
    if not payload:
        return None
    trip_id = payload.get("tripId")
    return int(trip_id) if trip_id is not None else None

# ---------------------------------------------------------------------------
# HTTP — действия с поездкой
# ---------------------------------------------------------------------------

def create_trip(passenger: PassengerService) -> dict:
    payload = {
        "originAddress": ORIGIN_ADDRESS,
        "originLat":     ORIGIN_LAT,
        "originLng":     ORIGIN_LNG,
        "destAddress":   DEST_ADDRESS,
        "destLat":       DEST_LAT,
        "destLng":       DEST_LNG,
    }
    res = requests.post(
        f"{API_URL}/trips",
        headers=passenger._auth_headers(),
        json=payload,
        timeout=30,
    )
    if res.status_code not in [200, 201]:
        fail(f"Создание поездки: {res.status_code} {res.text}")
    data = res.json()
    ok(f"Поездка создана: id={data['id']}, статус={data.get('status')}")

    tariffs = data.get("tariffs", [])
    if tariffs:
        info("Доступные тарифы:")
        for t in tariffs:
            info(f"  {t.get('tripClass')} — "
                 f"{t.get('prices', {}).get('price')} $ "
                 f"({t.get('driversNearby', 0)} водителей рядом)")
    return data


def start_search(passenger: PassengerService, trip_id: int) -> None:
    res = requests.post(
        f"{API_URL}/trips/{trip_id}/start-search",
        headers=passenger._auth_headers(),
        params={"vehicleClass": VEHICLE_CLASS},
        timeout=15,
    )
    if res.status_code not in [200, 204]:
        fail(f"start-search: {res.status_code} {res.text}")
    ok(f"Поиск водителя запущен для trip={trip_id}")


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

def get_trip_status(passenger: PassengerService, trip_id: int) -> Optional[str]:
    """Получить статус поездки через GET /trips/{id}"""
    try:
        res = requests.get(
            f"{API_URL}/trips/{trip_id}",
            headers=passenger._auth_headers(),
            timeout=5,
        )
        if res.status_code == 200:
            return res.json().get("status")
        return None
    except Exception as e:
        info(f"GET /trips/{trip_id} error: {e}")
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

    passenger = PassengerService(
        state_file=STATE_FILE,
        email_override=args.passenger_email,
    )
    if not passenger.login():
        fail("Пассажир не смог авторизоваться")
    ok(f"Пассажир авторизован: {passenger.email}")

    # ------------------------------------------------------------------
    # Шаг 2: Привязка карты пассажиру
    # ------------------------------------------------------------------
    step(2, "Привязка карты пассажиру")

    if args.no_card:
        info("Пропуск привязки карты (--no-card)")
    else:
        if passenger.attach_card(set_as_default=True):
            ok("Карта привязана")
        else:
            info("WARNING: Карта не привязана — поездка может отмениться при assignDriver")

    # ------------------------------------------------------------------
    # Шаг 3: Водитель
    # ------------------------------------------------------------------
    step(3, "Подготовка водителя")

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
    # Шаг 3.5: Привязка payout account водителю
    # ------------------------------------------------------------------
    step("3.5", "Привязка payout account водителю")

    if args.no_payout:
        info("Пропуск привязки payout account (--no-payout)")
    else:
        if driver.attach_payout_account():
            ok("Payout account готов")
        else:
            info("WARNING: Payout account не привязан — водитель не получит выплату")
    # ------------------------------------------------------------------
    # Шаг 4: WS-подключение водителя
    # ------------------------------------------------------------------
    step(4, "WebSocket подключение водителя")

    offer_event = threading.Event()
    received_trip_id: list[Optional[int]] = [None]

    def on_driver_message(message: str):
        event_type = extract_event_type(message)
        info(f"WS водителя: eventType={event_type}")

        if event_type == "TRIP_OFFER":
            trip_id = extract_trip_id(message)
            info(f"Получен оффер: tripId={trip_id}")
            received_trip_id[0] = trip_id
            offer_event.set()

    driver.on_ws_message = on_driver_message
    driver.connect_ws(block=False)
    time.sleep(2)
    ok("WS водителя подключён")

    # ------------------------------------------------------------------
    # Шаг 5: Водитель → ONLINE
    # ------------------------------------------------------------------
    step(5, "Водитель переходит в ONLINE")

    if not driver.go_online():
        fail("Не удалось перевести водителя в ONLINE")
    ok("Водитель ONLINE")

    info("Ожидание обновления локации в Redis (10с)...")
    time.sleep(10)

    driver_id = driver.state.get("driver_id")
    redis_status = check_redis_driver_status(driver_id)
    if redis_status is None:
        fail("Статус водителя НЕ в Redis! Проверь user-service и WebSocket.")
    if redis_status != "ONLINE":
        fail(f"Статус в Redis = {redis_status}, ожидалось ONLINE")
    ok(f"Redis статус подтверждён: {redis_status}")

    # ------------------------------------------------------------------
    # Шаг 6: Пассажир создаёт поездку
    # ------------------------------------------------------------------
    step(6, "Пассажир создаёт поездку")

    trip_data = create_trip(passenger)
    trip_id   = trip_data["id"]

    # ------------------------------------------------------------------
    # Шаг 7: Пассажир запускает поиск
    # ------------------------------------------------------------------
    step(7, "Пассажир запускает поиск водителя")

    start_search(passenger, trip_id)

    # ------------------------------------------------------------------
    # Шаг 8: Ждём оффер на WS водителя
    # ------------------------------------------------------------------
    step(8, f"Ожидание оффера водителю (до {WS_OFFER_TIMEOUT}с)")

    offer_arrived = offer_event.wait(timeout=WS_OFFER_TIMEOUT)

    if not offer_arrived:
        fail(
            f"Водитель не получил оффер за {WS_OFFER_TIMEOUT}с.\n"
            "  Проверь: водитель ONLINE, координаты в радиусе поиска,\n"
            "  notification-service запущен, RabbitMQ работает."
        )

    ws_trip_id = received_trip_id[0]
    if ws_trip_id is not None and ws_trip_id != trip_id:
        info(f"WARN: tripId в уведомлении ({ws_trip_id}) ≠ созданному ({trip_id})")
    ok(f"Оффер получен! tripId={trip_id}")

    # ------------------------------------------------------------------
    # Шаг 9: Водитель принимает оффер
    # ------------------------------------------------------------------
    step(9, "Водитель принимает оффер")

    status_before = check_redis_driver_status(driver_id)
    if status_before != "ONLINE":
        fail(f"Статус перед Accept = {status_before}, ожидается ONLINE")

    time.sleep(1)

    if not driver_accept_trip(driver, trip_id):
        fail("Водитель не смог принять поездку")

    status_after = check_redis_driver_status(driver_id)
    info(f"Статус после Accept = {status_after}")
    time.sleep(2)

    # ------------------------------------------------------------------
    # Шаг 10: Водитель стартует поездку
    # ------------------------------------------------------------------
    step(10, "Водитель стартует поездку")

    if not driver_start_trip(driver, trip_id):
        fail("Не удалось стартовать поездку")
    time.sleep(2)

    # ------------------------------------------------------------------
    # Шаг 11: Водитель завершает поездку
    # ------------------------------------------------------------------
    step(11, "Водитель завершает поездку")

    if not driver_complete_trip(driver, trip_id):
        fail("Не удалось завершить поездку")

    # ------------------------------------------------------------------
    # Шаг 12: Ожидание подтверждения оплаты
    # ------------------------------------------------------------------
    step(12, "Ожидание подтверждения оплаты")

    max_wait = 15
    start = time.time()
    completed = False

    while time.time() - start < max_wait:
        trip_status = get_trip_status(passenger, trip_id)

        if trip_status == "COMPLETED":
            ok("Поездка завершена (status=COMPLETED)")
            completed = True
            break
        elif trip_status == "PAYMENT_PENDING":
            info(f"Ожидание оплаты... (status={trip_status})")
        elif trip_status:
            info(f"Статус: {trip_status}")
        else:
            info("Не удалось получить статус поездки")

        time.sleep(2)

    if not completed:
        fail("Поездка не перешла в COMPLETED за 15с")

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
        "--passenger-email", type=str, default=None,
        help="Email конкретного пассажира из state.json (по умолчанию — случайный)"
    )
    parser.add_argument(
        "--no-card", action="store_true",
        help="Не привязывать карту (если уже привязана)"
    )
    parser.add_argument(
        "--no-payout", action="store_true",
        help="Не привязывать payout account водителю (если уже привязан)"
    )
    args = parser.parse_args()
    run(args)
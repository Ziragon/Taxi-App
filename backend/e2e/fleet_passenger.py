"""
fleet_passenger.py — запуск и управление несколькими пассажирами

Запуск:
    python fleet_passenger.py                     # все пассажиры из state.json
    python fleet_passenger.py --count 3           # первые 3 пассажира

Команды консоли:
    list                   — список пассажиров и их WS-статус
    status N               — профиль пассажира #N
    card   all / N         — привязать карту
    order  all / N         — создать поездку (запрашивает адреса и координаты)
    search all / N         — запустить поиск водителя
    boom                   — order all + search all подряд (массовый тест)
    cancel all / N         — отменить текущую поездку
    active all / N         — активная поездка
    trip   N <trip_id>     — поездка по ID
    history N              — история поездок
    notify                 — показать накопленные уведомления
    help
    exit
"""

import argparse
import json
import queue
import threading
import time

from passenger_service import PassengerService, DEFAULT_STATE_FILE


def _parse_stomp(raw: str) -> dict | None:
    """Извлекает JSON-тело из STOMP MESSAGE фрейма."""
    try:
        body = raw.split("\n\n", 1)[1].rstrip("\x00").strip()
        return json.loads(body)
    except Exception:
        return None


def _fmt_notify(item: dict) -> str:
    """Компактная строка для одного уведомления."""
    data = _parse_stomp(item["msg"])
    if data:
        event   = data.get("eventType", "?")
        trip_id = data.get("tripId", "?")
        message = data.get("message", "")
        return (
            f"  [{item['time']}] 🔔 #{item['idx']} {item['email']}\n"
            f"           event={event}  trip={trip_id}  {message}"
        )
    return f"  [{item['time']}] 🔔 #{item['idx']} {item['email']}\n  {item['msg'][:120]}"

HELP_TEXT = """
╔════════════════════════════════════════════════════╗
║         Fleet Passenger — команды консоли          ║
╠════════════════════════════════════════════════════╣
║  list                 — список пассажиров          ║
║  status N             — профиль пассажира #N       ║
║  card   all / N       — привязать тестовую карту   ║
║  order  all / N       — создать поездку            ║
║  search all / N       — запустить поиск водителя   ║
║  boom                 — order all + search all     ║
║  cancel all / N       — отменить поездку           ║
║  active all / N       — активная поездка           ║
║  trip   N <trip_id>   — поездка по ID              ║
║  history N            — история поездок            ║
║  notify               — показать уведомления       ║
║  help                 — эта справка                ║
║  exit                 — выход                      ║
╚════════════════════════════════════════════════════╝
"""

# ── Маршрут по умолчанию (Новосибирск) ───────────────────────────────────────
DEFAULT_ORIGIN_ADDR = "Новосибирск, Красный проспект, 1"
DEFAULT_ORIGIN_LAT  = 55.0302
DEFAULT_ORIGIN_LNG  = 82.9204
DEFAULT_DEST_ADDR   = "Новосибирск, Речной вокзал"
DEFAULT_DEST_LAT    = 54.9870
DEFAULT_DEST_LNG    = 82.8974


def _parse_coords(s: str):
    try:
        parts = s.replace(" ", "").split(",")
        return float(parts[0]), float(parts[1])
    except Exception:
        return None, None


class FleetPassengerManager:
    def __init__(self, state_file: str, count: int | None = None):
        self.state_file = state_file
        self.passengers: list[PassengerService] = []
        self._notify_queue: queue.Queue = queue.Queue()
        self._load_passengers(count)

    # ----------------------------------------------------------------- init --

    def _load_passengers(self, count: int | None):
        with open(self.state_file) as f:
            data = json.load(f)

        all_passengers = data.get("passengers", [])
        if not all_passengers:
            raise ValueError("В state.json нет пассажиров.")

        if count is not None:
            all_passengers = all_passengers[:count]

        print(f"[*] Инициализация {len(all_passengers)} пассажиров...")

        for profile in all_passengers:
            email = profile["passenger_email"]
            try:
                svc = PassengerService(
                    state_file=self.state_file,
                    email_override=email,
                )
                self.passengers.append(svc)
                print(f"    [{len(self.passengers)}] {email}")
            except Exception as e:
                print(f"    [!] Не удалось создать сервис для {email}: {e}")

    # ----------------------------------------------------------------- login -

    def login_all(self):
        ok = 0
        for svc in self.passengers:
            if svc.login():
                ok += 1
        print(f"[*] Авторизовано: {ok}/{len(self.passengers)}")

    # --------------------------------------------------------------- websocket

    def connect_all_ws(self):
        for idx, svc in enumerate(self.passengers, 1):
            if not svc.token:
                continue

            def make_cb(i, email):
                def cb(msg):
                    self._notify_queue.put({
                        "idx":   i,
                        "email": email,
                        "msg":   msg,
                        "time":  time.strftime("%H:%M:%S"),
                    })
                return cb

            svc.on_ws_message = make_cb(idx, svc.email)
            svc.connect_ws(block=False)

        time.sleep(1.5)

    # ----------------------------------------------------------------- utils --

    def _resolve(self, token: str) -> list[PassengerService]:
        if token == "all":
            return list(self.passengers)
        if token.isdigit():
            idx = int(token)
            if 1 <= idx <= len(self.passengers):
                return [self.passengers[idx - 1]]
            print(f"  [!] Нет пассажира #{idx}")
        else:
            print(f"  [!] Укажи номер или 'all'")
        return []

    def _get_passenger(self, token: str) -> PassengerService | None:
        svcs = self._resolve(token)
        return svcs[0] if svcs else None

    def _ask_route(self) -> tuple:
        """Интерактивный ввод маршрута, Enter = дефолтные значения."""
        print("\n  --- Маршрут (Enter = значения по умолчанию) ---")

        o_addr = input(f"  Адрес отправления [{DEFAULT_ORIGIN_ADDR}]: ").strip() or DEFAULT_ORIGIN_ADDR
        o_raw  = input(f"  Координаты отправления [{DEFAULT_ORIGIN_LAT}, {DEFAULT_ORIGIN_LNG}]: ").strip()
        o_lat, o_lng = _parse_coords(o_raw) if o_raw else (DEFAULT_ORIGIN_LAT, DEFAULT_ORIGIN_LNG)

        d_addr = input(f"  Адрес назначения [{DEFAULT_DEST_ADDR}]: ").strip() or DEFAULT_DEST_ADDR
        d_raw  = input(f"  Координаты назначения [{DEFAULT_DEST_LAT}, {DEFAULT_DEST_LNG}]: ").strip()
        d_lat, d_lng = _parse_coords(d_raw) if d_raw else (DEFAULT_DEST_LAT, DEFAULT_DEST_LNG)

        if None in (o_lat, o_lng, d_lat, d_lng):
            print("  [!] Неверный формат координат.")
            return None

        return o_addr, o_lat, o_lng, d_addr, d_lat, d_lng

    # --------------------------------------------------------------- commands -

    def cmd_list(self):
        print(f"\n  {'#':<4} {'Email':<45} {'WS':^6} {'Trip ID':>10}")
        print("  " + "─" * 70)
        for i, svc in enumerate(self.passengers, 1):
            ws_ok = svc.ws_thread and svc.ws_thread.is_alive()
            ws_icon = "🟢" if ws_ok else "🔴"
            trip = str(svc.current_trip_id) if svc.current_trip_id else "—"
            print(f"  {i:<4} {svc.email:<45} {ws_icon:^6} {trip:>10}")
        print()

    def cmd_status(self, token: str):
        svc = self._get_passenger(token)
        if not svc:
            return
        ws_ok = svc.ws_thread and svc.ws_thread.is_alive()
        print(f"  Email   : {svc.email}")
        print(f"  ID      : {svc.state.get('passenger_id', '—')}")
        print(f"  Trip ID : {svc.current_trip_id or 'Нет'}")
        print(f"  WS      : {'🟢 подключён' if ws_ok else '🔴 отключён'}")

    def cmd_card(self, target: str):
        for svc in self._resolve(target):
            svc.attach_card(set_as_default=True)

    def cmd_order(self, target: str, route: tuple):
        o_addr, o_lat, o_lng, d_addr, d_lat, d_lng = route
        svcs = self._resolve(target)
        if len(svcs) == 1:
            svcs[0].create_trip(o_addr, o_lat, o_lng, d_addr, d_lat, d_lng)
        else:
            # параллельный запуск для всех
            threads = [
                threading.Thread(
                    target=svc.create_trip,
                    args=(o_addr, o_lat, o_lng, d_addr, d_lat, d_lng),
                    daemon=True,
                )
                for svc in svcs
            ]
            for t in threads:
                t.start()
            for t in threads:
                t.join()

    def cmd_search(self, target: str, vehicle_class: str = "COMFORT"):
        svcs = self._resolve(target)
        if len(svcs) == 1:
            svcs[0].start_search(vehicle_class)
        else:
            threads = [
                threading.Thread(target=svc.start_search, args=(vehicle_class,), daemon=True)
                for svc in svcs
            ]
            for t in threads:
                t.start()
            for t in threads:
                t.join()

    def cmd_boom(self):
        """Массовый тест: одновременно order + search для всех пассажиров."""
        route = self._ask_route()
        if route is None:
            return

        v_class = input("  Класс авто (ECONOMY/COMFORT/BUSINESS) [COMFORT]: ").strip().upper() or "COMFORT"

        print(f"\n[*] BOOM: создаём поездки для {len(self.passengers)} пассажиров одновременно...")
        o_addr, o_lat, o_lng, d_addr, d_lat, d_lng = route

        def _order_and_search(svc: PassengerService):
            if svc.create_trip(o_addr, o_lat, o_lng, d_addr, d_lat, d_lng):
                time.sleep(0.3)   # небольшая пауза между create и start-search
                svc.start_search(v_class)

        threads = [
            threading.Thread(target=_order_and_search, args=(svc,), daemon=True)
            for svc in self.passengers
        ]
        for t in threads:
            t.start()
        for t in threads:
            t.join()
        print(f"[✔] BOOM завершён. Проверь notify для уведомлений.")

    def cmd_cancel(self, target: str):
        for svc in self._resolve(target):
            if svc.current_trip_id:
                svc.cancel_trip(str(svc.current_trip_id))
            else:
                # спросить ID вручную только для одиночного вызова
                raw = input(f"  ID поездки для {svc.email}: ").strip()
                if raw.isdigit():
                    svc.cancel_trip(raw)

    def cmd_active(self, target: str):
        for svc in self._resolve(target):
            svc.get_active_trip()

    def cmd_trip(self, token: str, trip_id: str):
        svc = self._get_passenger(token)
        if svc:
            svc.get_trip(trip_id)

    def cmd_history(self, token: str):
        raw_page = input("  Страница [0]: ").strip()
        raw_size = input("  Размер [10]: ").strip()
        page = int(raw_page) if raw_page.isdigit() else 0
        size = int(raw_size) if raw_size.isdigit() else 10
        svc = self._get_passenger(token)
        if svc:
            svc.get_history(page=page, size=size)

    def cmd_notify(self):
        count = 0
        while not self._notify_queue.empty():
            item = self._notify_queue.get_nowait()
            count += 1
            print(_fmt_notify(item))
        if count == 0:
            print("  [·] Новых уведомлений нет.")
        else:
            print(f"\n  Всего: {count} уведомлений.")

    def disconnect_all(self):
        for svc in self.passengers:
            svc.disconnect_ws()


# ────────────────────────────────────────────────── консоль ───────────────────

def main():
    parser = argparse.ArgumentParser(description="Fleet Passenger Manager")
    parser.add_argument("--count", type=int, default=None, help="Кол-во пассажиров (по умолчанию все)")
    parser.add_argument("--state", type=str, default=DEFAULT_STATE_FILE)
    args = parser.parse_args()

    manager = FleetPassengerManager(state_file=args.state, count=args.count)
    manager.login_all()
    manager.connect_all_ws()

    print(HELP_TEXT)

    while True:
        try:
            raw = input("fleet-passenger> ").strip()
        except (EOFError, KeyboardInterrupt):
            print("\n[*] Прерывание. Выход...")
            break

        if not raw:
            continue

        parts = raw.split()
        cmd   = parts[0].lower()

        # ── list ──────────────────────────────────────────────────────────────
        if cmd == "list":
            manager.cmd_list()

        # ── status N ──────────────────────────────────────────────────────────
        elif cmd == "status" and len(parts) == 2:
            manager.cmd_status(parts[1])

        # ── card all / N ──────────────────────────────────────────────────────
        elif cmd == "card" and len(parts) == 2:
            manager.cmd_card(parts[1])

        # ── order all / N ─────────────────────────────────────────────────────
        elif cmd == "order" and len(parts) == 2:
            route = manager._ask_route()
            if route:
                manager.cmd_order(parts[1], route)

        # ── search all / N ────────────────────────────────────────────────────
        elif cmd == "search" and len(parts) == 2:
            v_class = input("  Класс авто (ECONOMY/COMFORT/BUSINESS) [COMFORT]: ").strip().upper() or "COMFORT"
            manager.cmd_search(parts[1], v_class)

        # ── boom ──────────────────────────────────────────────────────────────
        elif cmd == "boom":
            manager.cmd_boom()

        # ── cancel all / N ────────────────────────────────────────────────────
        elif cmd == "cancel" and len(parts) == 2:
            manager.cmd_cancel(parts[1])

        # ── active all / N ────────────────────────────────────────────────────
        elif cmd == "active" and len(parts) == 2:
            manager.cmd_active(parts[1])

        # ── trip N <trip_id> ──────────────────────────────────────────────────
        elif cmd == "trip" and len(parts) == 3:
            manager.cmd_trip(parts[1], parts[2])

        # ── history N ─────────────────────────────────────────────────────────
        elif cmd == "history" and len(parts) == 2:
            manager.cmd_history(parts[1])

        # ── notify ────────────────────────────────────────────────────────────
        elif cmd == "notify":
            manager.cmd_notify()

        # ── help / exit ───────────────────────────────────────────────────────
        elif cmd == "help":
            print(HELP_TEXT)

        elif cmd == "exit":
            break

        else:
            print(f"  [?] Неизвестная команда или неверное число аргументов. Введи help.")

    manager.disconnect_all()
    print("[*] Сессия завершена.")


if __name__ == "__main__":
    main()
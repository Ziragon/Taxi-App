"""
fleet_driver.py — запуск и управление несколькими водителями

Запуск:
    python fleet_driver.py                        # все водители из state.json
    python fleet_driver.py --count 3              # первые 3 водителя
    python fleet_driver.py --count 3 --online     # сразу перевести в ONLINE

Координаты по умолчанию — центр Новосибирска, разброс ~10 км.

Команды консоли:
    list                  — список водителей и их WS-статус
    online all            — все → ONLINE
    online 2              — водитель #2 → ONLINE
    offline all / offline 2
    status all / status 2 — запросить статус с сервера
    accept 2 42           — водитель #2 принимает поездку #42
    reject 2 42
    start  2 42
    complete 2 42
    active 2              — активная поездка водителя #2
    trip   2 42           — поездка #42 глазами водителя #2
    history 2             — история поездок водителя #2
    notify                — показать накопленные уведомления
    help
    exit
"""

import argparse
import json
import queue
import random
import threading
import time

from driver_service import DriverService, DEFAULT_STATE_FILE


def _parse_stomp(raw: str) -> dict | None:
    """Извлекает JSON-тело из STOMP MESSAGE фрейма."""
    try:
        # тело идёт после первой пустой строки
        body = raw.split("\n\n", 1)[1].rstrip("\x00").strip()
        return json.loads(body)
    except Exception:
        return None


def _fmt_notify(item: dict) -> str:
    """Компактная строка для одного уведомления."""
    data = _parse_stomp(item["msg"])
    if data:
        event    = data.get("eventType", "?")
        trip_id  = data.get("tripId", "?")
        message  = data.get("message", "")
        return (
            f"  [{item['time']}] 🔔 #{item['idx']} {item['email']}\n"
            f"           event={event}  trip={trip_id}  {message}"
        )
    # fallback — если не распарсилось
    return f"  [{item['time']}] 🔔 #{item['idx']} {item['email']}\n  {item['msg'][:120]}"

# ── Новосибирск: центр + разброс ±0.09° ≈ 10 км ──────────────────────────────
NSK_LAT    = 55.0302
NSK_LNG    = 82.9204
NSK_SCATTER = 0.09        # градусы (~10 км по широте и долготе в Сибири)

HELP_TEXT = """
╔═══════════════════════════════════════════════════╗
║          Fleet Driver — команды консоли           ║
╠═══════════════════════════════════════════════════╣
║  list               — список водителей            ║
║  online  all / N    — перейти в ONLINE            ║
║  offline all / N    — перейти в OFFLINE           ║
║  status  all / N    — статус с сервера            ║
║  accept  N <trip>   — принять поездку             ║
║  reject  N <trip>   — отклонить поездку           ║
║  start   N <trip>   — начать поездку              ║
║  complete N <trip>  — завершить поездку           ║
║  active  N          — активная поездка            ║
║  trip    N <trip>   — поездка по ID               ║
║  history N          — история поездок             ║
║  card    all / N    — привязать тестовую карту    ║
║  notify             — показать уведомления        ║
║  help               — эта справка                 ║
║  exit               — выход                       ║
╚═══════════════════════════════════════════════════╝
"""


def _scatter_coords():
    lat = NSK_LAT + random.uniform(-NSK_SCATTER, NSK_SCATTER)
    lng = NSK_LNG + random.uniform(-NSK_SCATTER, NSK_SCATTER)
    return round(lat, 6), round(lng, 6)


class FleetDriverManager:
    def __init__(self, state_file: str, count: int | None = None):
        self.state_file = state_file
        self.drivers: list[DriverService] = []
        self._notify_queue: queue.Queue = queue.Queue()
        self._load_drivers(count)

    # ----------------------------------------------------------------- init --

    def _load_drivers(self, count: int | None):
        with open(self.state_file) as f:
            data = json.load(f)

        all_drivers = data.get("drivers", [])
        if not all_drivers:
            raise ValueError("В state.json нет водителей.")

        if count is not None:
            all_drivers = all_drivers[:count]

        print(f"[*] Инициализация {len(all_drivers)} водителей...")

        for profile in all_drivers:
            email = profile["driver_email"]
            lat, lng = _scatter_coords()
            try:
                svc = DriverService(
                    state_file=self.state_file,
                    lat=lat,
                    lng=lng,
                    email_override=email,
                )
                self.drivers.append(svc)
                print(f"    [{len(self.drivers)}] {email}  ({lat:.4f}, {lng:.4f})")
            except Exception as e:
                print(f"    [!] Не удалось создать сервис для {email}: {e}")

    # ----------------------------------------------------------------- login -

    def login_all(self):
        ok = 0
        for svc in self.drivers:
            if svc.login():
                ok += 1
        print(f"[*] Авторизовано: {ok}/{len(self.drivers)}")

    # --------------------------------------------------------------- websocket

    def connect_all_ws(self):
        for idx, svc in enumerate(self.drivers, 1):
            if not svc.token:
                continue
            # подменяем on_ws_message: вместо print — кладём в очередь
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
            # подавляем прямой print из WS
            svc.connect_ws(block=False)

        time.sleep(1.5)  # дать WS-потокам подняться

    # ----------------------------------------------------------------- utils --

    def _resolve(self, token: str) -> list[DriverService]:
        """Возвращает список сервисов по 'all' или номеру."""
        if token == "all":
            return list(self.drivers)
        if token.isdigit():
            idx = int(token)
            if 1 <= idx <= len(self.drivers):
                return [self.drivers[idx - 1]]
            print(f"  [!] Нет водителя #{idx}")
        else:
            print(f"  [!] Укажи номер или 'all'")
        return []

    def _get_driver(self, token: str) -> DriverService | None:
        svcs = self._resolve(token)
        return svcs[0] if svcs else None

    # --------------------------------------------------------------- commands -

    def cmd_list(self):
        print(f"\n  {'#':<4} {'Email':<40} {'WS':^6} {'Lat':>10} {'Lng':>10}")
        print("  " + "─" * 75)
        for i, svc in enumerate(self.drivers, 1):
            ws_ok = svc.ws_thread and svc.ws_thread.is_alive()
            ws_icon = "🟢" if ws_ok else "🔴"
            print(f"  {i:<4} {svc.email:<40} {ws_icon:^6} {svc.start_lat:>10.4f} {svc.start_lng:>10.4f}")
        print()

    def cmd_online(self, target: str):
        for svc in self._resolve(target):
            svc.go_online()

    def cmd_offline(self, target: str):
        for svc in self._resolve(target):
            svc.go_offline()

    def cmd_status(self, target: str):
        for svc in self._resolve(target):
            s = svc.get_status()
            icons = {"ONLINE": "🟢", "OFFLINE": "🔴", "BUSY": "🟡"}
            print(f"  {svc.email}: {icons.get(s, '❓')} {s}")

    def cmd_card(self, target: str):
        for svc in self._resolve(target):
            svc.attach_card()

    def cmd_accept(self, driver_token: str, trip_id: str):
        svc = self._get_driver(driver_token)
        if svc:
            svc.accept_trip(trip_id)

    def cmd_reject(self, driver_token: str, trip_id: str):
        svc = self._get_driver(driver_token)
        if svc:
            svc.reject_trip(trip_id)

    def cmd_start(self, driver_token: str, trip_id: str):
        svc = self._get_driver(driver_token)
        if svc:
            svc.start_trip(trip_id)

    def cmd_complete(self, driver_token: str, trip_id: str):
        svc = self._get_driver(driver_token)
        if svc:
            svc.complete_trip(trip_id)

    def cmd_active(self, driver_token: str):
        svc = self._get_driver(driver_token)
        if svc:
            svc.get_active_trip()

    def cmd_trip(self, driver_token: str, trip_id: str):
        svc = self._get_driver(driver_token)
        if svc:
            svc.get_trip(trip_id)

    def cmd_history(self, driver_token: str):
        raw_page = input("  Страница [0]: ").strip()
        raw_size = input("  Размер [10]: ").strip()
        page = int(raw_page) if raw_page.isdigit() else 0
        size = int(raw_size) if raw_size.isdigit() else 10
        svc = self._get_driver(driver_token)
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
        for svc in self.drivers:
            svc.disconnect_ws()


# ─────────────────────────────────────────────── консоль ──────────────────────

def main():
    parser = argparse.ArgumentParser(description="Fleet Driver Manager")
    parser.add_argument("--count",  type=int, default=None, help="Кол-во водителей (по умолчанию все)")
    parser.add_argument("--state",  type=str, default=DEFAULT_STATE_FILE)
    parser.add_argument("--online", action="store_true", help="Сразу перевести всех в ONLINE")
    args = parser.parse_args()

    manager = FleetDriverManager(state_file=args.state, count=args.count)
    manager.login_all()
    manager.connect_all_ws()

    if args.online:
        print("[*] --online: переводим всех в ONLINE...")
        manager.cmd_online("all")

    print(HELP_TEXT)

    while True:
        try:
            raw = input("fleet-driver> ").strip()
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

        # ── online / offline ──────────────────────────────────────────────────
        elif cmd == "online" and len(parts) == 2:
            manager.cmd_online(parts[1])

        elif cmd == "offline" and len(parts) == 2:
            manager.cmd_offline(parts[1])

        # ── status ────────────────────────────────────────────────────────────
        elif cmd == "status" and len(parts) == 2:
            manager.cmd_status(parts[1])

        # ── card all / N ──────────────────────────────────────────────────────
        elif cmd == "card" and len(parts) == 2:
            manager.cmd_card(parts[1])

        # ── accept / reject / start / complete  N <trip_id> ──────────────────
        elif cmd == "accept" and len(parts) == 3:
            manager.cmd_accept(parts[1], parts[2])

        elif cmd == "reject" and len(parts) == 3:
            manager.cmd_reject(parts[1], parts[2])

        elif cmd == "start" and len(parts) == 3:
            manager.cmd_start(parts[1], parts[2])

        elif cmd == "complete" and len(parts) == 3:
            manager.cmd_complete(parts[1], parts[2])

        # ── active / trip / history  N ────────────────────────────────────────
        elif cmd == "active" and len(parts) == 2:
            manager.cmd_active(parts[1])

        elif cmd == "trip" and len(parts) == 3:
            manager.cmd_trip(parts[1], parts[2])

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
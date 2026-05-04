"""
driver_ui.py - консоль для управления водителем онлайн

Запуск:
    python driver_ui.py
    python driver_ui.py --email driver_adf7b1da@example.com
    python driver_ui.py --email driver_adf7b1da@example.com --lat 55.75 --lng 37.61
"""

import argparse
import time
from driver_service import DriverService, DEFAULT_STATE_FILE

HELP_TEXT = """
╔══════════════════════════════════════╗
║     Driver UI — команды консоли      ║
╠══════════════════════════════════════╣
║  on      - перейти в ONLINE          ║
║  off     - перейти в OFFLINE         ║
║  status  - показать текущий профиль  ║
║  accept  - принять поездку по ID     ║
║  reject  - отклонить поездку по ID   ║
║  start   - начать поездку по ID      ║
║  complete- завершить поездку по ID   ║
║  card    - привязать тестовую карту  ║
║  help    - эта справка               ║
║  exit    - выход                     ║
╚══════════════════════════════════════╝
"""


def main():
    parser = argparse.ArgumentParser(description="Driver UI — локальная консоль")
    parser.add_argument("--lat",   type=float, default=55.755864)
    parser.add_argument("--lng",   type=float, default=37.617617)
    parser.add_argument("--email", type=str,   default=None)
    parser.add_argument("--state", type=str,   default=DEFAULT_STATE_FILE)
    args = parser.parse_args()

    service = DriverService(
        state_file=args.state,
        lat=args.lat,
        lng=args.lng,
        email_override=args.email,
    )

    # Логин
    if not service.login():
        print("[!] Не удалось авторизоваться. Выход.")
        return

    # WS в фоне
    service.connect_ws(block=False)

    # Ждём STOMP CONNECTED, чтобы консоль открылась после успешного подключения
    time.sleep(1.5)

    print(HELP_TEXT)

    while True:
        try:
            cmd = input("driver> ").strip().lower()
        except (EOFError, KeyboardInterrupt):
            print("\n[*] Прерывание. Выход...")
            break

        if cmd == "on":
            service.go_online()

        elif cmd == "off":
            service.go_offline()

        elif cmd == "status":
            print(f"  Email : {service.email}")
            print(f"  ID    : {service.state.get('driver_id', '—')}")
            print(f"  Token : {service.token[:50]}..." if service.token else "  Token : нет")
            ws_alive = service.ws_thread and service.ws_thread.is_alive()
            print(f"  WS    : {'🟢 подключён' if ws_alive else '🔴 отключён'}")
            current = service.get_status()
            icons = {"ONLINE": "🟢", "OFFLINE": "🔴", "BUSY": "🟡"}
            icon = icons.get(current, "❓")
            print(f"  Статус: {icon} {current if current else 'нет ответа'}")

        elif cmd == "card":
            service.attach_card()

        elif cmd == "accept":
            trip_id = input("  Введите ID поездки для принятия: ").strip()
            if trip_id.isdigit():
                route_data = service.accept_trip(trip_id)
                if isinstance(route_data, dict):
                    dist = route_data.get('distanceKm', 0)
                    dur = route_data.get('durationMin', 0)
                    print(f"  [ℹ] Дистанция до подачи: {dist} км")
                    print(f"  [ℹ] Примерное время: {dur:.2f} мин")
            else:
                print("  [!] Некорректный ID поездки.")

        elif cmd == "reject":
            trip_id = input("  Введите ID поездки для отклонения: ").strip()
            if trip_id.isdigit():
                service.reject_trip(trip_id)
            else:
                print("  [!] Некорректный ID поездки.")

        elif cmd == "start":
            trip_id = input("  Введите ID поездки для старта: ").strip()
            if trip_id.isdigit():
                service.start_trip(trip_id)
            else:
                print("  [!] Некорректный ID поездки.")

        elif cmd == "complete":
            trip_id = input("  Введите ID поездки для завершения: ").strip()
            if trip_id.isdigit():
                service.complete_trip(trip_id)
            else:
                print("  [!] Некорректный ID поездки.")

        elif cmd == "help":
            print(HELP_TEXT)

        elif cmd == "exit":
            break

        elif cmd == "":
            continue

        else:
            print(f"  [?] Неизвестная команда '{cmd}'. Введи help.")

    service.disconnect_ws()
    print("[*] Сессия завершена.")


if __name__ == "__main__":
    main()
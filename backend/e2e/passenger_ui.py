"""
passenger_ui.py - консоль для управления пассажиром

Запуск:
    python passenger_ui.py
    python passenger_ui.py --email passenger_123@example.com
"""

import argparse
from passenger_service import PassengerService, DEFAULT_STATE_FILE

HELP_TEXT = """
╔══════════════════════════════════════╗
║    Passenger UI — команды консоли    ║
╠══════════════════════════════════════╣
║  status  - показать текущий профиль  ║
║  card    - привязать тестовую карту  ║
║  order   - создать поездку           ║
║  search  - запустить поиск водителя  ║
║  cancel  - отменить поездку          ║
║  help    - эта справка               ║
║  exit    - выход                     ║
╚══════════════════════════════════════╝
"""

def parse_coords(input_str):
    """Парсит строку 'lat, lng' в кортеж float"""
    try:
        parts = input_str.replace(" ", "").split(",")
        return float(parts[0]), float(parts[1])
    except:
        return None, None

def main():
    parser = argparse.ArgumentParser(description="Passenger UI")
    parser.add_argument("--email", type=str, default=None)
    parser.add_argument("--state", type=str, default=DEFAULT_STATE_FILE)
    args = parser.parse_args()

    try:
        service = PassengerService(
            state_file=args.state,
            email_override=args.email
        )
    except Exception as e:
        print(f"[!] Ошибка инициализации: {e}")
        return

    # Логин
    if not service.login():
        print("[!] Не удалось авторизоваться. Выход.")
        return

    print(HELP_TEXT)

    while True:
        try:
            cmd = input("passenger> ").strip().lower()
        except (EOFError, KeyboardInterrupt):
            print("\n[*] Прерывание. Выход...")
            break

        if cmd == "status":
            print(f"  Email    : {service.email}")
            print(f"  ID       : {service.state.get('passenger_id', '—')}")
            print(f"  Trip ID  : {service.current_trip_id if service.current_trip_id else 'Нет'}")
            print(f"  Token    : {service.token[:50]}..." if service.token else "  Token : нет")

        elif cmd == "card":
            # Установим как дефолтную для удобства последующих оплат
            service.attach_card(set_as_default=True)

        elif cmd == "order":
            print("\n--- Создание поездки ---")
            origin_addr = input("  Адрес отправления: ") or "Новосибирск, 52"

            o_coords_input = input("  Координаты отправления (lat, lng) [54.8427, 83.0916]: ")
            o_lat, o_lng = parse_coords(o_coords_input) if o_coords_input else (54.8427, 83.0916)

            dest_addr = input("  Адрес назначения: ") or "Новосибирск, 42"

            d_coords_input = input("  Координаты назначения (lat, lng) [55.0302, 82.9366]: ")
            d_lat, d_lng = parse_coords(d_coords_input) if d_coords_input else (55.0302, 82.9366)

            if None in (o_lat, o_lng, d_lat, d_lng):
                print("  [!] Неверный формат координат. Используйте формат: 55.123, 82.123")
                continue

            service.create_trip(origin_addr, o_lat, o_lng, dest_addr, d_lat, d_lng)

        elif cmd == "search":
            v_class = input("  Класс авто (ECONOMY/COMFORT/BUSINESS) [COMFORT]: ").strip().upper() or "COMFORT"
            service.start_search(vehicle_class=v_class)

        elif cmd == "cancel":
            service.cancel_trip()

        elif cmd == "help":
            print(HELP_TEXT)

        elif cmd == "exit":
            break

        elif cmd == "":
            continue

        else:
            print(f"  [?] Неизвестная команда '{cmd}'. Введи help.")

if __name__ == "__main__":
    main()
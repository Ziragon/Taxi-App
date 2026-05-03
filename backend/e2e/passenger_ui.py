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
║  help    - эта справка               ║
║  exit    - выход                     ║
╚══════════════════════════════════════╝
"""

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
            print(f"  Email : {service.email}")
            print(f"  ID    : {service.state.get('passenger_id', '—')}")
            print(f"  Token : {service.token[:50]}..." if service.token else "  Token : нет")

        elif cmd == "card":
            service.attach_card()

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
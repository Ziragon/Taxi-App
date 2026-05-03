import json
import os
import requests

BASE_URL = os.getenv("TAXI_API_URL", "http://localhost:8000/api/v1")
STATE_FILE = "state.json"

ADMIN_CREDENTIALS = {
    "email": "admin@example.com",
    "password": "AdminPass123"
}

def load_state():
    if not os.path.exists(STATE_FILE):
        print(f"[!] Файл {STATE_FILE} не найден.")
        exit(1)
    with open(STATE_FILE, "r") as f:
        data = json.load(f)
        if isinstance(data, dict):
            return data
        return {"drivers": data, "passengers": []}

def admin_flow():
    state_data = load_state()
    drivers_list = state_data.get("drivers", [])

    if not drivers_list:
        print("[!] Список водителей пуст.")
        return

    print(f"[*] Авторизация админа...")
    login_res = requests.post(f"{BASE_URL}/auth/login", json=ADMIN_CREDENTIALS)
    if login_res.status_code != 200:
        print(f"[!] Ошибка входа админа: {login_res.text}")
        return

    admin_token = login_res.json()["accessTokenDto"]["token"]
    headers = {"Authorization": f"Bearer {admin_token}", "Content-Type": "application/json"}

    print(f"[*] Найдено профилей для верификации: {len(drivers_list)}")
    print("-" * 30)

    for entry in drivers_list:
        d_id = entry.get("driver_id")
        v_id = entry.get("vehicle_id")
        email = entry.get("driver_email", "unknown")

        print(f"[*] Обработка водителя: {email} (ID: {d_id})")

        # 1. Верификация машины
        if v_id:
            v_res = requests.post(f"{BASE_URL}/admin/vehicles/{v_id}/verify", headers=headers)
            if v_res.status_code in [200, 204]:
                print(f"  [+] Автомобиль {v_id} верифицирован")
            else:
                print(f"  [!] Ошибка верификации авто {v_id}: {v_res.status_code}")

        # 2. Верификация водителя
        if d_id:
            d_res = requests.post(f"{BASE_URL}/admin/drivers/{d_id}/verify", headers=headers)
            if d_res.status_code in [200, 204]:
                print(f"  [+] Профиль водителя {d_id} верифицирован")
            else:
                print(f"  [!] Ошибка верификации водителя {d_id}: {d_res.status_code}")

        print("-" * 30)

if __name__ == "__main__":
    admin_flow()
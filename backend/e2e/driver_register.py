import os
import random
import uuid
import requests
import pytest
import json

BASE_URL = os.getenv("TAXI_API_URL", "http://localhost:8000/api/v1")

class TestDriverRegistrationFlow:

    # Переменные для хранения состояния между шагами (тестами)
    access_token = None
    account_id = None
    vehicle_id = None
    current_email = None

    # Вспомогательные методы генерации данных
    def generate_email(self):
        return f"driver_{uuid.uuid4().hex[:8]}@example.com"

    def generate_phone(self):
        return f"+7{random.randint(9000000000, 9999999999)}"

    def get_auth_headers(self):
        """Возвращает заголовки с авторизацией для защищенных эндпоинтов"""
        return {
            "Authorization": f"Bearer {self.access_token}",
            "Content-Type": "application/json"
        }

    # --- Шаги E2E сценария ---

    def test_01_register_user(self):
        """Шаг 1: Регистрация нового пользователя и получение токена"""
        url = f"{BASE_URL}/auth/register"

        generated_email = self.generate_email()
        TestDriverRegistrationFlow.current_email = generated_email

        payload = {
            "email": generated_email,
            "phone": self.generate_phone(),
            "password": "SecurePass123"
        }

        response = requests.post(url, json=payload)
        assert response.status_code in [200, 201], f"Ошибка регистрации: {response.text}"

        data = response.json()
        assert "id" in data
        assert "accessTokenDto" in data

        TestDriverRegistrationFlow.account_id = data["id"]
        TestDriverRegistrationFlow.access_token = data["accessTokenDto"]["token"]

    def test_02_create_driver_profile(self):
        assert self.access_token is not None, "Токен не был получен на шаге регистрации"

        url = f"{BASE_URL}/profiles/driver"
        payload = {
            "firstName": "Сергей",
            "lastName": "Сидоров",
            "licenseNumber": f"77{random.randint(10000000, 99999999)}",
            "photoUrl": "https://cdn.example.com/driver.jpg"
        }

        response = requests.post(url, headers=self.get_auth_headers(), json=payload)

        assert response.status_code in [200, 201], f"Ошибка создания профиля: {response.text}"

        data = response.json()
        assert data["accountId"] == self.account_id
        assert data["status"] == "OFFLINE"

    def test_03_create_vehicle(self):
        assert self.access_token is not None

        url = f"{BASE_URL}/vehicles"
        payload = {
            "brand": "Toyota",
            "model": "Camry",
            "year": 2020,
            "color": "Черный",
            "licensePlate": f"А{random.randint(100, 999)}БВ75",
            "vehicleClass": "COMFORT"
        }

        response = requests.post(url, headers=self.get_auth_headers(), json=payload)

        assert response.status_code in [200, 201], f"Ошибка добавления авто: {response.text}"

        data = response.json()
        assert "id" in data
        assert data["driverId"] == self.account_id

        # Сохраняем ID машины для следующего шага
        TestDriverRegistrationFlow.vehicle_id = data["id"]

    def test_04_set_vehicle_active(self):
        """Шаг 4: Активация автомобиля"""
        assert self.access_token is not None
        assert self.vehicle_id is not None, "ID автомобиля не был получен на предыдущем шаге"

        url = f"{BASE_URL}/vehicles/{self.vehicle_id}/set-active"

        # Здесь тело запроса пустое
        response = requests.post(url, headers=self.get_auth_headers())

        # Так как ответа нет, ожидаем статус 200 OK или 204 No Content
        assert response.status_code in [200, 204], f"Ошибка активации авто: {response.text}"

    def test_05_save_state(self):
        """Добавляем данные нового водителя в структурированный state.json"""
        state_file = "state.json"

        # 1. Загружаем данные или инициализируем новую структуру
        if os.path.exists(state_file):
            with open(state_file, "r") as f:
                try:
                    state = json.load(f)
                    # Если внутри старый формат (просто список), конвертируем в новый
                    if isinstance(state, list):
                        state = {"drivers": state, "passengers": []}
                except:
                    state = {"drivers": [], "passengers": []}
        else:
            state = {"drivers": [], "passengers": []}

        # 2. Гарантируем наличие нужных ключей в словаре
        if "drivers" not in state:
            state["drivers"] = []
        if "passengers" not in state:
            state["passengers"] = []

        # 3. Подготавливаем данные нового водителя
        new_driver = {
            "driver_id": self.account_id,
            "vehicle_id": self.vehicle_id,
            "driver_email": self.current_email,
            "driver_password": "SecurePass123",
            "vehicle_class": "COMFORT"
        }

        # 4. Добавляем в список внутри ключа "drivers"
        state["drivers"].append(new_driver)

        # 5. Сохраняем ВЕСЬ словарь state обратно в файл
        with open(state_file, "w") as f:
            json.dump(state, f, indent=4)

        print(f"\n[+] Водитель {new_driver['driver_email']} добавлен в базу (drivers).")

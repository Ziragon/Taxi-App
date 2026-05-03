import os
import random
import uuid
import requests
import pytest
import json

BASE_URL = os.getenv("TAXI_API_URL", "http://localhost:8000/api/v1")

class TestPassengerRegistrationFlow:
    access_token = None
    account_id = None
    current_email = None

    def generate_email(self):
        return f"passenger_{uuid.uuid4().hex[:8]}@example.com"

    def generate_phone(self):
        return f"+7{random.randint(9000000000, 9999999999)}"

    def test_01_register_user(self):
        """Шаг 1: Регистрация аккаунта (общая для всех)"""
        url = f"{BASE_URL}/auth/register"
        self.__class__.current_email = self.generate_email()

        payload = {
            "email": self.current_email,
            "phone": self.generate_phone(),
            "password": "SecurePass123"
        }

        response = requests.post(url, json=payload)
        assert response.status_code in [200, 201]

        data = response.json()
        self.__class__.account_id = data["id"]
        self.__class__.access_token = data["accessTokenDto"]["token"]

    def test_02_create_passenger_profile(self):
        """Шаг 2: Создание профиля пассажира"""
        url = f"{BASE_URL}/profiles/passenger"
        headers = {
            "Authorization": f"Bearer {self.access_token}",
            "Content-Type": "application/json"
        }
        payload = {
            "firstName": "Иван",
            "lastName": "Иванов"
        }

        response = requests.post(url, headers=headers, json=payload)
        assert response.status_code in [200, 201]

    def test_03_save_state(self):
        """Шаг 3: Сохранение в обновленную структуру state.json"""
        state_file = "state.json"

        # Инициализация структуры, если файла нет
        if os.path.exists(state_file):
            with open(state_file, "r") as f:
                try:
                    state = json.load(f)
                except:
                    state = {"drivers": [], "passengers": []}
        else:
            state = {"drivers": [], "passengers": []}

        # Гарантируем наличие ключей
        if "passengers" not in state: state["passengers"] = []

        new_passenger = {
            "passenger_id": self.account_id,
            "passenger_email": self.current_email,
            "passenger_password": "SecurePass123",
            "first_name": "Иван",
            "last_name": "Иванов"
        }

        state["passengers"].append(new_passenger)

        with open(state_file, "w") as f:
            json.dump(state, f, indent=4)
        print(f"\n[+] Пассажир {new_passenger['passenger_email']} добавлен.")
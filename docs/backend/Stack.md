# Стек технологий и библиотек Taxi-App (backend)

Полный стек `backend/` 

---

## 1. Общие сведения

| Категория | Значение |
|-----------|----------|
| Язык | Java 25 (toolchain, `gradle.properties` → `javaVersion=25`, `base-convention.gradle.kts` → `JavaLanguageVersion.of(25)`) |
| DSL сборки | Kotlin DSL (`kotlin-dsl`, build-logic), Gradle 9.4.1 |
| Модель сборки | Composite build: `build-logic` (конвенции/плагины) + 5 сервисов + 3 shared-библиотеки |
| Сборка | Gradle wrapper 9.4.1 (`distributionUrl=...gradle-9.4.1-bin.zip`) |
| Runtime-образ | `eclipse-temurin:25-jre-alpine` (Dockerfile) |
| Фреймворк | Spring Boot 4.0.5 (Spring Framework 7.x) |
| Платформа облака | Spring Cloud 2025.1.1 |
| Менеджер зависимостей | Spring Boot BOM + `spring-dependency-management` 1.1.7 (в каталоге `[versions]`), Spring Cloud BOM |

---

## 2. Версии (version catalog `gradle/libs.versions.toml`)

| Компонент | Версия | Источник |
|-----------|--------|----------|
| Java | 25 | `[versions] java` |
| Spring Boot | 4.0.5 | `[versions] spring-boot`, `build-logic/build.gradle.kts` (spring-boot-gradle-plugin 4.0.5) |
| Spring Cloud | 2025.1.1 | `[versions] spring-cloud` |
| spring-dependency-management | 1.1.7 | `[versions] spring-dependency-management` |
| jjwt (api/impl/jackson) | 0.13.0 | `[versions] jjwt` |
| springdoc-openapi (webmvc-ui / webflux-ui) | 3.0.3 | `[versions] springdoc` |
| stripe-java | 32.0.0 | `[versions] stripe` |
| testcontainers (jdbc) | 1.19.7 | `[versions] testcontainers` |
| awaitility | 4.2.2 | `[libraries] awaitility` |

**Управляется Spring Boot BOM 4.0.5** :

| Библиотека | Комментарий |
|------------|-------------|
| lombok | 1.18.44 (указано комментарием в каталоге; версия из BOM) |
| jackson-databind | BOM |
| postgresql (JDBC-драйвер) | BOM |
| flyway-core, flyway-database-postgresql, spring-boot-starter-flyway | BOM |
| testcontainers-junit-jupiter, -postgresql, -rabbitmq, -redis (`com.redis:testcontainers-redis`), spring-boot-testcontainers, spring-boot-webmvc-test, spring-boot-data-jpa-test, spring-security-test | BOM |

---

## 3. Spring Boot/Cloud starters и библиотеки

### Стартеры (везде через BOM)
| Библиотека | Где применяется |
|------------|-----------------|
| spring-boot-starter | Все приложения (`spring-app`) |
| spring-boot-starter-web (WebMVC) | user/trip/payment/notification (`spring-service-webmvc`), shared-libs |
| spring-boot-starter-webflux | gateway (`spring-gateway`), shared-security (api) |
| spring-boot-starter-actuator | Все приложения (metrics/health/probes) |
| spring-boot-starter-validation | Все приложения + shared-libs |
| spring-boot-starter-security | user/trip/payment/gateway/notification (`security-jwt-plugin`), shared-security |
| spring-boot-starter-data-jpa | user/trip/payment/notification (`database-plugin`) |
| spring-boot-starter-data-redis | user/trip/notification (redis-plugin; payment тоже подключает через build-файл) |
| spring-boot-starter-cache | user/trip (redis-plugin) |
| spring-boot-starter-amqp | user/trip/payment/notification (`rabbitmq-plugin`) |
| spring-boot-starter-flyway | user/trip/payment/notification (`database-plugin`) |
| spring-cloud-starter-openfeign | user/trip/payment/notification (`openfeign-plugin`) |
| spring-cloud-starter-gateway-server-webflux | gateway (`spring-gateway`) |
| spring-boot-configuration-processor | Все приложения (annotationProcessor) |
| spring-websocket, spring-messaging | notification (`websocket-plugin`) |

### Прочие зависимости
| Библиотека | Версия | Где |
|------------|--------|-----|
| jjwt-api / jjwt-impl / jjwt-jackson | 0.13.0 | user/trip/payment/gateway (`security-jwt-plugin`), notification, shared-security |
| springdoc-openapi-starter-webmvc-ui | 3.0.3 | user/trip/payment/notification (`spring-service-webmvc`), shared-dto |
| springdoc-openapi-starter-webflux-ui | 3.0.3 | gateway (`spring-gateway`) |
| stripe-java | 32.0.0 | payment (`stripe-plugin`) — только конфиг/инициализация (реализация — STUB) |
| jackson-databind | BOM | shared-dto (api) |
| lombok | BOM (1.18.44) | Все модули (compileOnly + annotationProcessor) |
| awaitility | 4.2.2 | Тесты (`integration-testing`) |
| testcontainers (+ jdbc 1.19.7, junit-jupiter, postgresql, rabbitmq, redis) | BOM / 1.19.7 | Интеграционные тесты (`integration-testing`) |
| spring-boot-starter-test / boot-webmvc-test / boot-starter-jpa-test / spring-security-test | BOM | Тесты |

---

## 4. Инфраструктура (docker-compose.dev.yml)

| Сервис | Образ | Версия | Порт (host) |
|--------|-------|--------|-------------|
| PostgreSQL | `postgres:17-alpine` | 17 | 5433 → 5432 |
| Redis | `redis:alpine` | latest (alpine) | 6379 |
| RabbitMQ | `rabbitmq:4.0-management-alpine` | 4.0 (+ Management) | 5672 (AMQP), 15672 (UI) |
| OSRM (маршрутизация) | `ghcr.io/ziragon/osrm-novosibirsk:dev` (база `osrm/osrm-backend`, алгоритм mld, данные Новосибирска) | dev | 5000 |
| nginx | `nginx:alpine` | latest | 80, 443 (reverse proxy) |
| pgAdmin | `dpage/pgadmin4` | latest | 5050 → 80 |
| Redis Insight | `redis/redisinsight:latest` | latest | 8001 |

БД PostgreSQL (создаются в `docker/sql/init.sql`): `user_db`, `trip_db`, `payment_db`, `notification_db`.

Redis database index: user-service → 0, trip-service → 1, notification-service → 2.

---

## 5. Распределение по сервисам

| Сервис | Порт | Плагины/конвенции (build) | Доп. зависимости |
|--------|------|---------------------------|------------------|
| gateway-service | 8000 | spring-gateway, security-jwt-plugin, integration-testing | — |
| user-service | 8080 | spring-service-webmvc, security-jwt, database, redis, rabbitmq, openfeign, integration-testing | shared-exceptions, shared-security, shared-dto |
| trip-service | 8081 | spring-service-webmvc, security-jwt, database, redis, rabbitmq, openfeign, integration-testing | shared-exceptions, shared-security, shared-dto |
| payment-service | 8082 | spring-service-webmvc, security-jwt, database, redis, rabbitmq, openfeign, integration-testing, stripe-plugin | shared-exceptions, shared-security, shared-dto |
| notification-service | 8083 | spring-service-webmvc, security-jwt, database, redis, rabbitmq, websocket, openfeign, integration-testing | shared-exceptions, shared-dto |
| shared-exceptions | — | spring-library | — |
| shared-dto | — | spring-library | jackson-databind, springdoc-webmvc-ui (api) |
| shared-security | — | spring-library, security-jwt-plugin | boot-starter-webflux (api), openfeign (compileOnly) |

---

## 6. Внешние API / сервисы

| Внешняя система | Endpoint | Использует |
|-----------------|----------|------------|
| OSRM (маршруты) | `http://localhost:5000` `/route/v1/driving/{coords}?overview=full` | trip-service (OpenFeign `OsrmClient`) |
| WeatherAPI.com | `https://api.weatherapi.com/v1/current.json` (ключ `${WEATHER_API_KEY}`) | trip-service (OpenFeign `WeatherAPIClient`) |
| Stripe | SDK `stripe-java` (ключ `${STRIPE_API_KEY}`) | payment-service — **заглушка**, реальных вызовов нет |

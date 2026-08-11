# Функциональность бэкенда Taxi-App 

Реализация в `backend/` (5 сервисов + shared-libs).

---

## 1. АУТЕНТИФИКАЦИЯ И АВТОРИЗАЦИЯ

### user-service
Реализация:
- Регистрация создаёт аккаунт (роль USER), пароль хэшируется BCrypt, параллельно в RabbitMQ уходит событие `user.registered` (routing key, exchange `user.events`).
- Вход по email+пароль, при неактивном аккаунте — `AccountDeactivatedException`.
- JWT access-токен: sub = accountId, claim `role`, issuer из конфига, подпись HMAC-SHA256 (секрет из `gateway.secret`), TTL 30m.
- Refresh-токен: UUID, в БД хранится только SHA-256 хэш (Base64), ротация (старый токен помечается revoked, выдаётся новая пара), TTL 7d.
- Logout отзывает все refresh-токены аккаунта; access-токен остаётся валиден до истечения (stateless).

Ключевые классы/файлы: AuthService (backend/user-service/src/main/java/com/example/userservice/service/AuthService.java), AccountService (…/service/AccountService.java), TokenService (…/service/TokenService.java), JwtUtil (…/util/JwtUtil.java), AuthController (…/controller/AuthController.java)
Используемые библиотеки: Spring Security, jjwt (api/impl/jackson), spring-boot-starter-amqp, BCrypt (Spring Security Crypto)

Endpoints:
1. POST /api/v1/auth/register — Регистрация пассажира (возвращает access+refresh)
2. POST /api/v1/auth/login — Вход по email и паролю
3. POST /api/v1/auth/refresh — Ротация refresh-токена, выдача новой пары
4. POST /api/v1/auth/logout — Отзыв всех refresh-токенов пользователя

### gateway-service
Реализация:
- `JwtAuthenticationFilter` (GlobalFilter, order -1): блокирует любой путь, содержащий `/internal/` (403), для open-путей пропускает анонимно, иначе требует `Authorization: Bearer`, валидирует JWT (issuer + подпись), из claim `role` формирует заголовки `X-User-Id`, `X-User-Role`, `X-User-Anonymous`, `X-Gateway-Header` (заголовок-ключ для downstream).
- Маршрутизация: `/api/v1/auth/**`, `/api/v1/driver/**`, `/api/v1/profiles/**`, `/api/v1/vehicles/**`, `/api/v1/admin/**` → user-service (8080); `/api/v1/trips/**` → trip-service (8081); `/api/v1/payment-methods/**`, `/api/v1/payout-accounts/**`, `/api/v1/transactions/**` → payment-service (8082). Swagger: агрегированный UI на 8000.
- CORS из конфига `cors.allowed-origins`.

Ключевые классы/файлы: JwtAuthenticationFilter (backend/gateway-service/src/main/java/com/example/gatewayservice/security/JwtAuthenticationFilter.java), JwtUtil (…/util/JwtUtil.java), SecurityConfig (…/config/SecurityConfig.java), AppProperties (…/config/AppProperties.java)
Используемые библиотеки: spring-cloud-starter-gateway (WebFlux), jjwt, springdoc-webflux-ui

Endpoints:
1. Все методы /api/v1/auth/**, /api/v1/driver/**, /api/v1/profiles/**, /api/v1/vehicles/**, /api/v1/admin/** — Маршрут → user-service
2. Все методы /api/v1/trips/** — Маршрут → trip-service
3. Все методы /api/v1/payment-methods/**, /api/v1/payout-accounts/**, /api/v1/transactions/** — Маршрут → payment-service
4. GET /user-service/v3/api-docs, /trip-service/v3/api-docs, /payment-service/v3/api-docs — Swagger docs (RewritePath)
5. Все методы /actuator/**, /swagger-ui/**, /v3/api-docs/** и др. — Open-пути без JWT

### shared-security (используется user/trip/payment-service)
Реализация:
- `GatewayAuthFilter` (OncePerRequestFilter): пропускает запрос только при наличии `X-Gateway-Header` = headerKey (от gateway) или `X-Internal-Header` = headerKey (внутренние вызовы). Для запросов от gateway создаёт `UserPrincipal(userId, role)` в SecurityContext; анонимный флаг из `X-User-Anonymous`. Исключения (shouldNotFilter): `/actuator`, `/v3/api-docs`, `/swagger-ui`, пути `/api/v1/*/internal`.
- `InternalFeignConfig`: RequestInterceptor добавляет внутренний заголовок (`X-Internal-Header`) во все Feign-запросы.
- Автоконфигурация `GatewaySecurityAutoConfiguration` регистрирует фильтр и properties (`gateway-auth.*`, `internal-auth.*`).

Ключевые классы/файлы: GatewayAuthFilter (backend/shared-libs/shared-security/src/main/java/com/example/shared/security/GatewayAuthFilter.java), InternalFeignConfig (…/security/InternalFeignConfig.java), GatewayAuthProperties (…/security/GatewayAuthProperties.java), InternalAuthProperties (…/security/InternalAuthProperties.java), GatewaySecurityAutoConfiguration (…/security/GatewaySecurityAutoConfiguration.java), UserPrincipal (…/security/UserPrincipal.java)

### notification-service (WebSocket-аутентификация)
Реализация:
- `JwtHandshakeInterceptor`: JWT передаётся query-параметром `token` на `/ws/notifications`, валидируется (parseTokenSilently), в session attributes кладутся userId/userRole.
- `StompAuthChannelInterceptor`: на CONNECT читает session attributes, выставляет UserPrincipal; ограничивает отправку в `/app/driver/location` только для DRIVER.

Ключевые классы/файлы: JwtHandshakeInterceptor (backend/notification-service/src/main/java/com/example/notificationservice/websocket/JwtHandshakeInterceptor.java), StompAuthChannelInterceptor (…/websocket/StompAuthChannelInterceptor.java), JwtUtil (…/security/JwtUtil.java)
Используемые библиотеки: spring-websocket, spring-messaging, jjwt

---

## 2. УПРАВЛЕНИЕ АККАУНТАМИ И ПРОФИЛЯМИ

### user-service
Реализация:
- Аккаунт: email/phone (уникальны), passwordHash, role (USER/ADMIN), active. Создание/поиск/блокировка/разблокировка.
- Профиль пассажира: создаётся отдельно после регистрации, averageRating/totalTrips инициализируются нулями; `getProfile` кэшируется (@Cacheable).
- Профиль водителя: создаётся отдельно, `is_verified=false`, status=OFFLINE по умолчанию; дубликаты (id/номер ВУ) отклоняются.
- Обновление рейтинга водителя/пассажира: `RatingCalculator.calculate` — скользящее среднее (avg*(n) + new) / (n+1), HALF_UP 2 знака.

Ключевые классы/файлы: AccountService (backend/user-service/src/main/java/com/example/userservice/service/AccountService.java), PassengerProfileService (…/service/PassengerProfileService.java), DriverProfileService (…/service/DriverProfileService.java), RatingCalculator (…/util/RatingCalculator.java), PassengerProfileController (…/controller/PassengerProfileController.java), DriverProfileController (…/controller/DriverProfileController.java)
Используемые библиотеки: Spring Data JPA, spring-boot-starter-cache, Spring Security (BCrypt)

Endpoints:
1. POST /api/v1/profiles/passenger — Создать профиль пассажира
2. GET /api/v1/profiles/passenger — Получить профиль пассажира
3. PUT /api/v1/profiles/passenger — Обновить имя/фамилию/фото
4. POST /api/v1/profiles/driver — Создать профиль водителя (is_verified=false)
5. GET /api/v1/profiles/driver — Получить профиль водителя
6. PUT /api/v1/profiles/driver — Обновить профиль водителя

---

## 3. ТРАНСПОРТНЫЕ СРЕДСТВА

### user-service
Реализация:
- Автомобиль водителя: brand/model/year/color/licensePlate (уникален)/vehicleClass, по умолчанию is_active=false.
- Ownership-проверка при обновлении/удалении; активным может быть только один автомобиль (остальные сбрасываются).
- Удаление активного авто при наличии активной поездки блокируется через `TripStatusService.validateDriverStatus` (запрос в trip-service).

Ключевые классы/файлы: VehicleService (backend/user-service/src/main/java/com/example/userservice/service/VehicleService.java), VehicleController (…/controller/VehicleController.java)
Используемые библиотеки: Spring Data JPA, openfeign

Endpoints:
1. POST /api/v1/vehicles — Добавить автомобиль
2. GET /api/v1/vehicles — Список автомобилей водителя
3. PUT /api/v1/vehicles/{vehicleId} — Обновить автомобиль (только владелец)
4. POST /api/v1/vehicles/{vehicleId}/set-active — Сделать автомобиль активным
5. DELETE /api/v1/vehicles/{vehicleId} — Удалить автомобиль (только владелец)

---

## 4. ВОДИТЕЛИ: СТАТУСЫ И ГЕОЛОКАЦИЯ

### user-service
Реализация:
- Статусы ONLINE/OFFLINE/BUSY хранятся в Redis (`driver:status:{id}`), при ONLINE даётся TTL (heartbeat), при OFFLINE ключ удаляется вместе с гео/локацией.
- Геолокация: `driver:location:{id}` (TTL), GEO-индекс `drivers:geo`, ZSET `drivers:online:heartbeat` (score = timestamp). `@Scheduled` раз в 30с чистит stale-водителей (старше 60с).
- Поиск ближайших онлайн-водителей: geo search (сортировка по дистанции, limit 50), фильтр по статусу ONLINE и VehicleClass, mget локаций.
- `DriverLocationService.persistLocationsToDatabase` (@Scheduled раз в 60с) пишет историю локаций онлайн-водителей в БД (таблица driver_locations).
- `goOnline` проверяет: верификацию, active аккаунт, наличие активного авто, отсутствие активной поездки (Feign → trip-service).

Ключевые классы/файлы: DriverCachingService (backend/user-service/src/main/java/com/example/userservice/service/DriverCachingService.java), DriverLocationService (…/service/DriverLocationService.java), DriverProfileService (…/service/DriverProfileService.java), TripStatusService (…/service/TripStatusService.java), DriverStatusController (…/controller/DriverStatusController.java), DriverLocationInternalController (…/controller/DriverLocationInternalController.java), DriverStatusInternalController (…/controller/DriverStatusInternalController.java)
Используемые библиотеки: spring-boot-starter-data-redis (GEO, ZSET), spring-boot-starter-cache, openfeign

Endpoints:
1. PUT /api/v1/driver/online — Начало смены (ONLINE)
2. PUT /api/v1/driver/offline — Конец смены (OFFLINE, чистка кэша)
3. GET /api/v1/driver/status — Текущий статус
4. PUT /api/v1/internal/drivers/{driverId}/location — Обновить локацию (internal)
5. GET /api/v1/internal/drivers/nearby — Ближайшие онлайн-водители (internal)
6. PUT /api/v1/internal/driver/{driverId}/status/offline — Принудительно OFFLINE (internal)
7. PUT /api/v1/internal/driver/{driverId}/status/busy — Пометить BUSY (internal)

### notification-service (передача локации)
Реализация:
- `DriverLocationHandler` (@MessageMapping `/driver/location`): принимает локацию по WebSocket (только от DRIVER), проксирует в user-service (`PUT /api/v1/internal/drivers/{driverId}/location`).
- `StompSessionEventListener`: при дисконнекте DRIVER вызывает `setDriverOffline` в user-service.

Ключевые классы/файлы: DriverLocationHandler (backend/notification-service/src/main/java/com/example/notificationservice/websocket/DriverLocationHandler.java), StompSessionEventListener (…/websocket/StompSessionEventListener.java), UserServiceClient (…/client/UserServiceClient.java)

---

## 5. АДМИНИСТРИРОВАНИЕ

### user-service
Реализация:
- Все admin-эндпоинты под `@PreAuthorize("hasAuthority('ADMIN')")`.
- Аккаунты: список, блокировка/разблокировка (is_active).
- Водители: список, верификация (is_verified=true).
- Пассажиры: список.
- Автомобили: список, верификация, удаление.
- При старте приложения (профиль != test) `AdminInitializer` создаёт админа из `admin.email/phone/password`.

Ключевые классы/файлы: AdminService (backend/user-service/src/main/java/com/example/userservice/service/AdminService.java), AdminController (…/controller/AdminController.java), AdminInitializer (…/config/AdminInitializer.java), AdminProperties (…/config/AdminProperties.java)
Используемые библиотеки: Spring Security (method security)

Endpoints:
1. GET /api/v1/admin/accounts — Все аккаунты
2. PUT /api/v1/admin/accounts/{accountId}/deactivate — Заблокировать аккаунт
3. PUT /api/v1/admin/accounts/{accountId}/activate — Разблокировать аккаунт
4. GET /api/v1/admin/drivers — Все профили водителей
5. POST /api/v1/admin/drivers/{driverId}/verify — Верифицировать водителя
6. GET /api/v1/admin/passengers — Все профили пассажиров
7. GET /api/v1/admin/vehicles — Все автомобили
8. POST /api/v1/admin/vehicles/{vehicleId}/verify — Верифицировать автомобиль
9. DELETE /api/v1/admin/vehicles/{vehicleId} — Удалить автомобиль

---

## 6. ПОЕЗДКИ: ЧЕРНОВИК И РАСЧЁТ СТОИМОСТИ

### trip-service
Реализация:
- Создание черновика: проверка профиля пассажира (Feign user-service) и отсутствия активной поездки, параллельная загрузка маршрута (OSRM), погоды (WeatherAPI), ближайших водителей, активных тарифов (CompletableFuture + AsyncTaskExecutor).
- Черновик хранится в Redis (`passenger:{id}:trip_draft`, TTL 10 мин).
- Тарифы фильтруются: показываются только классы, по которым есть водители поблизости; в ответе — число доступных водителей.
- Формула цены: `(base_fare + distance*pricePerKm + duration*pricePerMin) * weatherCoef * surgeCoef`, не ниже minFare.
- Surge-коэффициент («имитация пробок»): ночь (0–6ч) 1.2, час-пик (8–10, 17–20) 1.15, иначе 1.0.
- Погодный коэффициент: WeatherAPIPriceUtil — нормальные коды 1.0, суровые (метель/гра/гроза/сильный дождь и т.д.) 1.2, промежуточные 1.1. Кэш weather (Redis, ключ по координатам ~11 км, TTL 10 мин).

Ключевые классы/файлы: TripCreationService (backend/trip-service/src/main/java/com/example/tripservice/service/trip/TripCreationService.java), TripDataAggregator (…/service/external/TripDataAggregator.java), PriceService (…/service/pricing/PriceService.java), TariffService (…/service/pricing/TariffService.java), WeatherAPIPriceUtil (…/util/WeatherAPIPriceUtil.java), WeatherService (…/service/external/WeatherService.java), NavigationService (…/service/external/NavigationService.java), TripDraftCacheService (…/service/cache/TripDraftCacheService.java)
Используемые библиотеки: Spring Data JPA, Redis (cache), OpenFeign (OSRM/Weather/user-service), jackson

Endpoints:
1. POST /api/v1/trips — Предварительный расчёт: черновик + тарифы (body: адреса и координаты)
2. POST /api/v1/trips/start-search — Выбор тарифа (query vehicleClass) и переход в SEARCHING

---

## 7. ПОИСК И НАЗНАЧЕНИЕ ВОДИТЕЛЯ

### trip-service
Реализация:
- Асинхронный поиск (@Async) по радиусам из конфига (`searching.radiuses` = 5,10,15 км): каждый шаг запрашивает ближайших онлайн-водителей у user-service.
- Водителю отправляется TripOfferEvent (RabbitMQ `notification.trip.offer`) + Redis pub/sub на канал `driver:response:{tripId}`.
- Ожидание ответа: CompletableFuture (timeout = `searching.duration` 15с) → результат ACCEPT/REJECT/TIMEOUT/CANCELLED. По таймауту публикуется `notification.trip.offer.expired`.
- Блокировка водителя на время оффера: `driver_busy:{driverId}` (setIfAbsent, TTL search+5), активный оффер: `active_offer:{tripId}`.
- При ACCEPT: проверка статуса водителя (должен быть ONLINE), верификация активного оффера, публикация ответа, статус водителя → BUSY, назначение водителя (DRIVER_ASSIGNED), проверка платёжного метода пассажира (при отсутствии — поездка отменяется).

Ключевые классы/файлы: DriverSearchService (backend/trip-service/src/main/java/com/example/tripservice/service/search/DriverSearchService.java), DriverResponsePublisher (…/service/search/DriverResponsePublisher.java), DriverResponseSubscriber (…/service/search/DriverResponseSubscriber.java), OfferCacheService (…/service/cache/OfferCacheService.java), TripOfferPublisher (…/messaging/TripOfferPublisher.java), ProfileStatusService (…/service/external/ProfileStatusService.java)
Используемые библиотеки: Redis pub/sub (RedisMessageListenerContainer, PatternTopic `driver:response:*`), RabbitMQ, CompletableFuture/AsyncTaskExecutor

Endpoints:
1. POST /api/v1/trips/{tripId}/accept — Водитель принимает заказ (возвращает маршрут до пассажира)
2. POST /api/v1/trips/{tripId}/reject — Водитель отклоняет заказ
3. GET /api/v1/internal/driver/{driverId}/trip-status — Есть ли активная поездка (internal, для user-service)

---

## 8. ПОЕЗДКИ: ЖИЗНЕННЫЙ ЦИКЛ

### trip-service
Реализация:
- Статусы: CREATED → SEARCHING → DRIVER_ASSIGNED → IN_PROGRESS → COMPLETED (или CANCELLED); в коде также есть PAYMENT_PENDING (в enum, но в сервисах не используется).
- `setSearching`: фиксирует тариф и цену, priceBreakdown (JSONB), сохраняет активную поездку пассажира в кэш (`passenger:{id}:active_trip`, TTL 2ч).
- `assignDriver`: назначает водителя, уведомление DRIVER_ASSIGNED, кэш активной поездки водителя.
- `startTrip`: DRIVER_ASSIGNED → IN_PROGRESS, уведомление TRIP_STARTED.
- `completeTrip`: IN_PROGRESS → COMPLETED, публикует TripCompletedEvent (RabbitMQ `trip.completed`), уведомления, очистка кэшей.
- Отмена пассажиром: нельзя при IN_PROGRESS/COMPLETED; при отмене в SEARCHING — отмена pending future и снятие оффера.
- `cancelSearch`: когда водители не найдены, отмена + уведомление SYSTEM.
- Активная поездка пассажира/водителя отдаётся из Redis-кэша + актуальный маршрут из OSRM.
- История: пагинация по пассажиру/водителю (created_at desc).

Ключевые классы/файлы: TripService (backend/trip-service/src/main/java/com/example/tripservice/service/trip/TripService.java), TripStatusService (…/service/trip/TripStatusService.java), TripCancellationService (…/service/trip/TripCancellationService.java), ActiveTripCacheService (…/service/cache/ActiveTripCacheService.java), TripPassengerController (…/controller/TripPassengerController.java), TripDriverController (…/controller/TripDriverController.java), TripInternalController (…/controller/TripInternalController.java), StatusValidationUtil (…/util/StatusValidationUtil.java)
Используемые библиотеки: Spring Data JPA, Redis, RabbitMQ, OpenFeign (OSRM/payment/user)

Endpoints:
1. POST /api/v1/trips/{tripId}/start — Начало поездки (водитель)
2. POST /api/v1/trips/{tripId}/complete — Завершение поездки (водитель)
3. POST /api/v1/trips/{tripId}/cancel — Отмена поездки (пассажир)
4. GET /api/v1/trips/{tripId} — Детали поездки (пассажир)
5. GET /api/v1/trips/driver/{tripId} — Детали поездки (водитель)
6. GET /api/v1/trips/active — Активная поездка пассажира
7. GET /api/v1/trips/driver-active — Активная поездка водителя (+ координаты)
8. GET /api/v1/trips/history — История пассажира (page/size)
9. GET /api/v1/trips/driver/history — История водителя (page/size)

---

## 9. ПЛАТЕЖИ И ТРАНЗАКЦИИ

### payment-service
Реализация:
- Stripe-интеграция — ЗАГЛУШКА (STUB): `StripeService` возвращает фейковые `cus_fake_`, `pi_fake_`, `re_fake_`, `acct_fake_`, `tr_fake_` id; карта `visa/4242`; `isAccountVerified` всегда true.
- Платёжные методы пассажира: привязка (Stripe PM id), первая карта = default, запрет удаления дефолтного при наличии других, дубликаты отклоняются.
- Счета выплат водителя: Stripe Connect аккаунт, верификация (sync/manual, admin-only), дефолтный счёт.
- Транзакции: CHARGE, REFUND, PAYOUT, HOLD. HOLD → capture в CHARGE при завершении поездки или release при отмене.
- События: `payment.succeeded`, `payment.failed`, `refund.succeeded`, `payout.succeeded` → exchange `payment.exchange` (и дубли в `notification.*` для уведомлений).

Ключевые классы/файлы: StripeService (backend/payment-service/src/main/java/com/example/paymentservice/service/StripeService.java), PaymentMethodService (…/service/PaymentMethodService.java), DriverPayoutAccountService (…/service/DriverPayoutAccountService.java), TransactionService (…/service/TransactionService.java), PaymentMethodController (…/controller/PaymentMethodController.java), DriverPayoutAccountController (…/controller/DriverPayoutAccountController.java), TransactionController (…/controller/TransactionController.java), PaymentEventPublisher (…/messaging/PaymentEventPublisher.java)
Используемые библиотеки: stripe-java (только конфиг/инициализация), Spring Data JPA, RabbitMQ

Endpoints:
1. POST /api/v1/payment-methods — Добавить карту (stripePaymentMethodId, setAsDefault)
2. GET /api/v1/payment-methods — Активные методы пассажира
3. GET /api/v1/payment-methods/default — Дефолтный метод
4. PUT /api/v1/payment-methods/{id}/set-default — Сделать дефолтным
5. DELETE /api/v1/payment-methods/{id} — Удалить метод
6. POST /api/v1/payout-accounts — Добавить счёт для выплат (lastFour)
7. GET /api/v1/payout-accounts — Счета водителя
8. GET /api/v1/payout-accounts/default — Дефолтный счёт
9. PUT /api/v1/payout-accounts/{id}/set-default — Сделать дефолтным (только верифицированный)
10. DELETE /api/v1/payout-accounts/{id} — Удалить счёт
11. POST /api/v1/payout-accounts/{id}/verify — Верифицировать (admin)
12. POST /api/v1/payout-accounts/{id}/sync — Синхронизировать верификацию (admin)
13. POST /api/v1/transactions/charge — Создать платёж
14. POST /api/v1/transactions/refund — Создать возврат
15. GET /api/v1/transactions/{id} — Транзакция по ID
16. GET /api/v1/transactions/trip/{tripId} — Транзакция по поездке
17. GET /api/v1/transactions/history/passenger — История пассажира
18. GET /api/v1/transactions/history/driver — История водителя
19. GET /api/v1/internal/payment-methods/passenger/{id}/default — Дефолтный метод (internal)
20. GET /api/v1/internal/payment-methods/{id} — Метод по ID (internal)
21. POST /api/v1/internal/transactions/charge — Платёж (internal)
22. POST /api/v1/internal/transactions/refund — Возврат (internal)
23. POST /api/v1/internal/transactions/payout — Выплата водителю (internal)
24. POST /api/v1/internal/transactions/hold — Заморозка средств (internal)
25. GET /api/v1/internal/transactions/trip/{tripId} — Транзакция по поездке (internal)
26. GET /api/v1/internal/payout-accounts/driver/{id}/default — Дефолтный верифицированный счёт (internal)
27. GET /api/v1/internal/payout-accounts/driver/{id}/has-verified — Есть верифицированный счёт (internal)

### trip-service (интеграция с оплатой)
Реализация:
- `startSearching` создаёт HOLD через payment-service (Feign); при Feign 404 (нет метода) поездка отменяется + `PaymentMethodNotFoundException`.
- `PaymentEventListener`: слушает `payment.succeeded/failed/refund.succeeded` → `TripPaymentService` (фиксирует paymentId, публикует уведомления).
- `TripEventPublisher`: `trip.completed` → payment-service; `refund.requested` → payment-service.

Ключевые классы/файлы: TripPaymentService (backend/trip-service/src/main/java/com/example/tripservice/service/trip/TripPaymentService.java), PaymentEventListener (…/messaging/PaymentEventListener.java), TripEventPublisher (…/messaging/TripEventPublisher.java), PaymentServiceClient (…/client/PaymentServiceClient.java)

### payment-service (события поездок)
Реализация:
- `TripEventListener` (Rabbit): на `trip.completed` → captureHold, при ошибке fallback на createCharge; далее автоматический payout водителю 80% от суммы (`DRIVER_SHARE`). На `refund.requested` → createRefund.

Ключевые классы/файлы: TripEventListener (backend/payment-service/src/main/java/com/example/paymentservice/messaging/TripEventListener.java), RabbitMQConfig (…/config/RabbitMQConfig.java)

---

## 10. РЕЙТИНГИ

### trip-service
Реализация:
- Оценка после COMPLETED, участником поездки (пассажир или водитель), без дубликатов (tripId+raterId+rateeId), SYSTEM не может оценивать.
- Отдельный эндпоинт `/api/v1/trips/ratings`; тип оценивающего — `AccountType` (PASSENGER/DRIVER/SYSTEM). Сохранение рейтинга в `ratings`. Прямого пересчёта среднего рейтинга в профилях нет в этом сервисе.

Ключевые классы/файлы: TripRatingService (backend/trip-service/src/main/java/com/example/tripservice/service/trip/TripRatingService.java), RatingController (…/controller/RatingController.java)

Endpoints:
1. POST /api/v1/trips/ratings — Оценить участника поездки (raterId, rateeId, tripId, accountType, score, comment)

---

## 11. УВЕДОМЛЕНИЯ И WEBSOCKET

### notification-service
Реализация:
- RabbitMQ consumer `UniversalNotificationConsumer` на `notification.queue` (exchange `notification.exchange` по `notification.#`, плюс `user.registered` из `user.events`): парсит событие по routing key, сохраняет в БД (PENDING), при необходимости подтягивает снапшоты профилей (Feign user-service), отправляет по WebSocket в `/queue/notifications` (STOMP) и помечает SENT/FAILED. При ошибке — исключение (ретрай) + запись FAILED. Есть DLX/DLQ (`notification.dlx`, `notification.dlq`).
- Поддерживаемые routing keys: user.registered, notification.trip.offer, notification.trip.offer.expired, notification.trip.driver_assigned/started/completed/cancelled, notification.payment.succeeded/failed, notification.payout.succeeded, notification.refund.succeeded.
- История уведомлений: GET по recipientId (desc), непрочитанные (SENT), чтение (по одному / все), счётчик непрочитанных.
- WebSocket: endpoint `/ws/notifications` (SockJS), CORS `allowed-origins`, `NotificationService.sendToUser` → `convertAndSendToUser(recipientId, /queue/notifications)`.

Ключевые классы/файлы: UniversalNotificationConsumer (backend/notification-service/src/main/java/com/example/notificationservice/consumer/UniversalNotificationConsumer.java), NotificationService (…/service/NotificationService.java), NotificationController (…/controller/NotificationController.java), WebSocketConfig (…/config/WebSocketConfig.java), RabbitMqConfig (…/config/RabbitMqConfig.java), Notification (…/entity/Notification.java)
Используемые библиотеки: spring-boot-starter-amqp, spring-websocket/spring-messaging, OpenFeign

Endpoints:
1. GET /api/v1/notifications — История уведомлений пользователя
2. GET /api/v1/notifications/unread — Непрочитанные (SENT)
3. POST /api/v1/notifications/{id}/read — Отметить прочитанным
4. POST /api/v1/notifications/read-all — Отметить все прочитанными
5. GET /api/v1/notifications/unread/count — Счётчик непрочитанных
6. WS /ws/notifications — STOMP endpoint (SockJS), destination /app/driver/location, subscription /user/queue/notifications

### trip-service (генерация уведомлений)
Реализация:
- `NotificationPublisher` публикует в `notification.exchange`: DRIVER_ASSIGNED, TRIP_STARTED, TRIP_COMPLETED, TRIP_CANCELLED (passenger/driver), PAYMENT_SUCCEEDED/PAYOUT (80% доля водителя), PAYMENT_FAILED, REFUND_SUCCEEDED.
- `TripOfferPublisher` — TripOfferEvent и OfferExpired.

Ключевые классы/файлы: NotificationPublisher (backend/trip-service/src/main/java/com/example/tripservice/messaging/NotificationPublisher.java)

---

## 12. ВНЕШНИЕ ИНТЕГРАЦИИ (OSRM, Погода)

### trip-service
Реализация:
- OSRM: маршрут `/route/v1/driving/{coords}?overview=full` → дистанция/длительность/geometry. Кэш `routes` (Redis, ключ = координаты, TTL 15 мин).
- WeatherAPI: `https://api.weatherapi.com/v1/current.json` (key из конфига). Кэш `weather` (Redis, ключ с округлением координат до ~11 км, TTL 10 мин). При ошибке — fallback с коэффициентом 1.0.
- Оба клиента — OpenFeign.

Ключевые классы/файлы: OsrmClient (backend/trip-service/src/main/java/com/example/tripservice/client/OsrmClient.java), WeatherAPIClient (…/client/WeatherAPIClient.java), NavigationService (…/service/external/NavigationService.java), WeatherService (…/service/external/WeatherService.java)
Используемые библиотеки: OpenFeign, spring-boot-starter-cache (Redis)

---

## 13. ИНФРАСТРУКТУРА И КОНФИГУРАЦИЯ

### Все сервисы (общее)
- Java 25 (toolchain), Gradle composite build `build-logic` с каталогами конвенций: `spring-app`, `spring-service-webmvc`, `spring-gateway`, `security-jwt-plugin`, `database-plugin`, `redis-plugin`, `rabbitmq-plugin`, `openfeign-plugin`, `integration-testing`, `websocket-plugin`, `stripe-plugin`, `spring-library`.
- БД: PostgreSQL 5433 (user_db, trip_db, payment_db, notification_db), Flyway миграции (baseline-on-migrate), Hibernate ddl-auto=validate.
- RabbitMQ: user.events, payment.exchange, trip.exchange, notification.exchange (см. отдельный файл). JacksonJsonMessageConverter.
- Redis: user-service DB 0, trip-service DB 1, notification-service DB 2.
- Metrics/health: actuator (health,info,metrics,prometheus), probes.
- Общая обработка ошибок: `AbstractExceptionHandler` (BaseException, 422 validation, 404, 500), `ErrorResponse`.
- OpenAPI: springdoc (swagger-ui включён только в gateway).

Ключевые классы/файлы: AbstractExceptionHandler (backend/shared-libs/shared-exceptions/src/main/java/com/example/shared/exception/base/AbstractExceptionHandler.java), конвенции (backend/build-logic/src/main/kotlin/*.gradle.kts)

---

## СВОДНАЯ ТАБЛИЦА ГОТОВНОСТИ

| Функциональность | Сервис | Статус |
|------------------|--------|--------|
| Регистрация/вход/refresh/logout, JWT | user-service | Реализовано |
| Маршрутизация, JWT-фильтр, заголовки | gateway-service | Реализовано |
| Защита downstream по заголовкам | shared-security (user/trip/payment) | Реализовано |
| WS-аутентификация по JWT | notification-service | Реализовано |
| Профили пассажира и водителя | user-service | Реализовано |
| Автомобили и верификация | user-service | Реализовано |
| Статусы и геолокация водителей (Redis GEO) | user-service | Реализовано |
| Передача локации через WS | notification-service | Реализовано |
| Администрирование | user-service | Реализовано |
| Черновик поездки и тарифы | trip-service | Реализовано |
| Поиск и назначение водителя | trip-service | Реализовано |
| Жизненный цикл поездки (статусы/отмена) | trip-service | Реализовано |
| Платёжные методы, счета, транзакции (Stripe-STUB) | payment-service | Реализовано (заглушка) |
| Оплата/возврат/выплата по событиям поездок | payment-service + trip-service | Реализовано |
| Рейтинги | trip-service | Реализовано |
| Уведомления (Rabbit + WebSocket + история) | notification-service | Реализовано |
| OSRM маршруты, погода | trip-service | Реализовано |

# Межсервисные взаимодействия Taxi-App

Все связи между сервисами `backend/`: HTTP-вызовы (Feign) и асинхронные события (RabbitMQ, Redis, WebSocket).

---

## 1. HTTP-вызовы (Feign) по internal-эндпоинтам

### user-service → trip-service
Клиент: `TripClient` (backend/user-service/src/main/java/com/example/userservice/client/TripClient.java), конфигурация `InternalFeignConfig` (заголовок добавляется).

| Метод | Путь | Назначение |
|-------|------|------------|
| GET | /api/v1/internal/driver/{driverId}/trip-status | Есть ли активная поездка (для goOnline, удаления активного авто при активной поездке) |

### trip-service → user-service
Клиент: `UserServiceClient` (backend/trip-service/src/main/java/com/example/tripservice/client/UserServiceClient.java), конфигурация `InternalFeignConfig` (заголовок добавляется).

| Метод | Путь | Назначение |
|-------|------|------------|
| GET | /api/v1/internal/drivers/nearby?lng&lat&rad&vehicleClass | Ближайшие онлайн-водители (поиск водителя) |
| GET | /api/v1/internal/passenger/{accountId}/exists | Есть ли профиль пассажира (создание черновика) |
| GET | /api/v1/internal/driver/{accountId}/status | Статус водителя (проверка ONLINE) |
| PUT | /api/v1/internal/driver/{driverId}/status/busy | Пометить водителя BUSY (назначение) |

### trip-service → payment-service
Клиент: `PaymentServiceClient` (backend/trip-service/src/main/java/com/example/tripservice/client/PaymentServiceClient.java), конфигурация `InternalFeignConfig` (заголовок добавляется).

| Метод | Путь | Назначение |
|-------|------|------------|
| GET | /api/v1/internal/payment-methods/passenger/{passengerId}/default | Дефолтный платёжный метод пассажира |
| GET | /api/v1/internal/payout-accounts/driver/{driverId}/has-verified | Есть ли верифицированный счёт выплат |
| POST | /api/v1/internal/transactions/charge | Создать платёж |
| POST | /api/v1/internal/transactions/refund | Создать возврат |
| POST | /api/v1/internal/transactions/payout | Выплата водителю |
| POST | /api/v1/internal/transactions/hold | Заморозка средств (HOLD) |
| GET | /api/v1/internal/transactions/trip/{tripId} | Транзакция по поездке |

### notification-service → user-service
Клиент: `UserServiceClient` (backend/notification-service/src/main/java/com/example/notificationservice/client/UserServiceClient.java).

**ВАЖНО**: заголовок `X-Internal-Header` НЕ добавляется — `InternalFeignConfig` не подключён, хотя user-service защищён фильтром `GatewayAuthFilter`.

| Метод | Путь | Назначение |
|-------|------|------------|
| PUT | /api/v1/internal/drivers/{driverId}/location | Обновление локации водителя (через WS /driver/location) |
| PUT | /api/v1/internal/driver/{driverId}/status/offline | Снять водителя с линии (дисконнект WS) |
| PUT | /api/v1/internal/driver/{driverId}/status/busy | Пометить BUSY |
| GET | /api/v1/internal/driver/{driverId}/profile | Снапшот профиля водителя (enrichment уведомлений) |
| GET | /api/v1/internal/passenger/{passengerId}/profile | Снапшот профиля пассажира (enrichment уведомлений) |

### dead code
- `TripServiceClient` (backend/payment-service/src/main/java/com/example/paymentservice/client/TripServiceClient.java) — Feign на `GET /api/v1/internal/trips/{tripId}`, объявлен в payment-service, но нигде не используется; соответствующего эндпоинта в trip-service нет.

---

## 2. Асинхронные события (RabbitMQ)

### Exchange `trip.exchange` (topic)
Производитель: trip-service (`TripEventPublisher`).

| Routing key | Очередь (получатель) | Событие |
|-------------|----------------------|---------|
| trip.completed | payment.trip.completed (payment-service) | Поездка завершена → capture hold / charge + payout |
| refund.requested | payment.refund.requested (payment-service) | Запрошен возврат → createRefund |

### Exchange `payment.exchange` (topic)
Производитель: payment-service (`PaymentEventPublisher`).

| Routing key | Очереди (получатели) | Событие |
|-------------|----------------------|---------|
| payment.succeeded | payment.succeeded (payment-service), trip.payment.succeeded (trip-service) | Платёж прошёл |
| payment.failed | payment.failed (payment-service), trip.payment.failed (trip-service) | Платёж не прошёл |
| refund.succeeded | payment.refund.succeeded (payment-service), trip.refund.succeeded (trip-service) | Возврат прошёл |
| payout.succeeded | payment.payout.succeeded (payment-service) | Выплата водителю прошла |

### Exchange `notification.exchange` (topic)
Производители: trip-service (`TripOfferPublisher`, `NotificationPublisher`). Получатель: notification-service (очередь `notification.queue`, binding `notification.#`).

| Routing key | Событие |
|-------------|---------|
| notification.trip.offer | Оффер водителю (поиск водителя) |
| notification.trip.offer.expired | Оффер истёк |
| notification.trip.driver_assigned | Водитель назначен |
| notification.trip.started | Поездка началась |
| notification.trip.completed | Поездка завершена |
| notification.trip.cancelled | Поездка отменена (пассажир/водитель) |
| notification.payment.succeeded | Оплата прошла |
| notification.payment.failed | Оплата не прошла |
| notification.payout.succeeded | Выплата водителю (80%) прошла |
| notification.refund.succeeded | Возврат прошёл |

### Exchange `user.events` (topic)
Производитель: user-service (`AuthService`). Получатель: notification-service (очередь `notification.queue`).

| Routing key | Событие |
|-------------|---------|
| user.registered | Пользователь зарегистрирован |

### DLX/DLQ notification-service
`notification.queue` → dead-letter exchange `notification.dlx` (routing key `notification.dead`) → очередь `notification.dlq`.

---

## 3. WebSocket / Redis

| Канал | Участники | Назначение |
|-------|-----------|------------|
| WS `/ws/notifications` (STOMP/SockJS) | notification-service (endpoint) ↔ клиенты (user-service не участвует) | Подписка `/user/queue/notifications`, отправка локации `/app/driver/location` |
| WS локация → user-service | notification-service → user-service (Feign) | Проксирование локации водителя + снятие с линии при дисконнекте |
| Redis pub/sub `driver:response:{tripId}` | trip-service (публикатор и подписчик, внутри одного сервиса) | Ответ водителя на оффер (ACCEPT/REJECT) |

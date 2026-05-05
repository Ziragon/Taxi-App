## Общая схема

```
Flutter App
    │
    │  1. HTTP POST /auth/login → получаем JWT токен
    │
    │  2. Открываем WebSocket соединение (передаём токен)
    │
    │  3. STOMP CONNECT (передаём userType)
    │
    │  4. Подписываемся на очередь уведомлений
    │
    │  5. Получаем уведомления в реальном времени
    │
    │  6. При логауте — закрываем соединение
    ▼
Backend (notification-service)
```

Конфигурация:
## !ПРИМЕР!

```
# pubspec.yaml
dependencies:
  stomp_dart_client: ^1.4.0  # STOMP поверх WebSocket
```

реальный WS URL — `/ws/notifications/websocket`

```
// lib/services/notification_service.dart

import 'dart:convert';
import 'package:stomp_dart_client/stomp_dart_client.dart';

class NotificationService {
  static const String _wsUrl = 'ws://YOUR_HOST/ws/notifications/websocket';

  StompClient? _stompClient;
  bool _isConnected = false;

  // Колбэки для обработки уведомлений
  final Map<String, Function(NotificationPayload)> _handlers = {};

  /// Подключение к WebSocket
  /// [token] — JWT токен пользователя
  /// [userType] — 'DRIVER' или 'PASSENGER'
  void connect({
    required String token,
    required String userType,
  }) {
    _stompClient = StompClient(
      config: StompConfig(
        url: '$_wsUrl?token=$token',   // ← JWT в query param

        // STOMP CONNECT заголовки
       
		stompConnectHeaders: {
		    'accept-version': '1.1',
		    'heart-beat': '10000,10000',
		    'host': 'YOUR_HOST',     // ← добавить
		    'userType': userType,
		},


        webSocketConnectHeaders: {
          'Authorization': 'Bearer $token',
        },

        onConnect: _onConnect,
        onDisconnect: _onDisconnect,
        onWebSocketError: _onError,
        onStompError: _onStompError,

        // Heartbeat
        heartbeatIncoming: const Duration(seconds: 10),
        heartbeatOutgoing: const Duration(seconds: 10),

        // Переподключение при обрыве
        reconnectDelay: const Duration(seconds: 5),
      ),
    );

    _stompClient!.activate();
  }

  void _onConnect(StompFrame frame) {
    _isConnected = true;
    print('[WS] Connected');

    // Подписка на персональную очередь уведомлений
    _stompClient!.subscribe(
      destination: '/user/queue/notifications',
      callback: _onMessage,
    );
  }

  void _onMessage(StompFrame frame) {
    if (frame.body == null) return;

    try {
      final Map<String, dynamic> json = jsonDecode(frame.body!);
      final payload = NotificationPayload.fromJson(json);

      print('[WS] Received: ${payload.eventType}');

      // Вызываем нужный обработчик
      final handler = _handlers[payload.eventType];
      handler?.call(payload);

      // Вызываем универсальный обработчик если есть
      _handlers['*']?.call(payload);

    } catch (e) {
      print('[WS] Failed to parse message: $e');
    }
  }

  void _onDisconnect(StompFrame frame) {
    _isConnected = false;
    print('[WS] Disconnected');
  }

  void _onError(dynamic error) {
    print('[WS] WebSocket error: $error');
  }

  void _onStompError(StompFrame frame) {
    print('[WS] STOMP error: ${frame.body}');
  }

  /// Регистрация обработчика для конкретного eventType
  /// Используй '*' для обработки всех событий
  void on(String eventType, Function(NotificationPayload) handler) {
    _handlers[eventType] = handler;
  }

  void disconnect() {
    _stompClient?.deactivate();
    _isConnected = false;
  }

  bool get isConnected => _isConnected;
}
```

## DTO — NotificationPayload

## !ПРИМЕР!

```
// lib/models/notification_payload.dart

class NotificationPayload {
  final int notificationId;
  final int? tripId;
  final String eventType;
  final String message;
  final DateTime createdAt;
  final DriverProfileSnapshot? driverProfile;
  final PassengerProfileSnapshot? passengerProfile;

  NotificationPayload({
    required this.notificationId,
    required this.tripId,
    required this.eventType,
    required this.message,
    required this.createdAt,
    this.driverProfile,
    this.passengerProfile,
  });

  factory NotificationPayload.fromJson(Map<String, dynamic> json) {
    return NotificationPayload(
      notificationId: json['notificationId'],
      tripId:         json['tripId'],
      eventType:      json['eventType'],
      message:        json['message'],
      createdAt:      DateTime.parse(json['createdAt']),
      driverProfile:  json['driverProfile'] != null
          ? DriverProfileSnapshot.fromJson(json['driverProfile'])
          : null,
      passengerProfile: json['passengerProfile'] != null
          ? PassengerProfileSnapshot.fromJson(json['passengerProfile'])
          : null,
    );
  }
}

class DriverProfileSnapshot {
  final int accountId;
  final String firstName;
  final String lastName;
  final String? photoUrl;
  final double averageRating;

  DriverProfileSnapshot({
    required this.accountId,
    required this.firstName,
    required this.lastName,
    this.photoUrl,
    required this.averageRating,
  });

  factory DriverProfileSnapshot.fromJson(Map<String, dynamic> json) {
    return DriverProfileSnapshot(
      accountId:     json['accountId'],
      firstName:     json['firstName'],
      lastName:      json['lastName'],
      photoUrl:      json['photoUrl'],
      averageRating: (json['averageRating'] as num).toDouble(),
    );
  }

  String get fullName => '$firstName $lastName';
}

class PassengerProfileSnapshot {
  final int accountId;
  final String firstName;
  final String lastName;
  final String? photoUrl;
  final double averageRating;

  PassengerProfileSnapshot({
    required this.accountId,
    required this.firstName,
    required this.lastName,
    this.photoUrl,
    required this.averageRating,
  });

  factory PassengerProfileSnapshot.fromJson(Map<String, dynamic> json) {
    return PassengerProfileSnapshot(
      accountId:     json['accountId'],
      firstName:     json['firstName'],
      lastName:      json['lastName'],
      photoUrl:      json['photoUrl'],
      averageRating: (json['averageRating'] as num).toDouble(),
    );
  }

  String get fullName => '$firstName $lastName';
}
```

## !ПРИМЕР!

## Обработка событий

### Пассажир


```
// lib/services/passenger_notification_handler.dart

class PassengerNotificationHandler {
  final NotificationService _ws;

  PassengerNotificationHandler(this._ws);

  void register() {
    // Водитель назначен — показать профиль водителя
    _ws.on('DRIVER_ASSIGNED', (payload) {
      final driver = payload.driverProfile;
      if (driver != null) {
        // Показать карточку водителя
        showDriverCard(
          name:   driver.fullName,
          rating: driver.averageRating,
          photo:  driver.photoUrl,
        );
      }
      showNotification(payload.message);
    });

    // Поездка началась
    _ws.on('TRIP_STARTED', (payload) {
      showNotification(payload.message);
      navigateToTripScreen(payload.tripId);
    });

    // Поездка завершена
    _ws.on('TRIP_COMPLETED', (payload) {
      showNotification(payload.message);
      navigateToRatingScreen(payload.tripId);
    });

    // Поездка отменена
    _ws.on('TRIP_CANCELLED', (payload) {
      showNotification(payload.message);
      navigateToHomeScreen();
    });

    // Оплата прошла
    _ws.on('PAYMENT_SUCCEEDED', (payload) {
      showNotification(payload.message);
    });

    // Ошибка оплаты
    _ws.on('PAYMENT_FAILED', (payload) {
      showError(payload.message);
      navigateToPaymentScreen();
    });

    // Возврат
    _ws.on('REFUND_SUCCEEDED', (payload) {
      showNotification(payload.message);
    });
  }
}
```


### Водитель


```
// lib/services/driver_notification_handler.dart

class DriverNotificationHandler {
  final NotificationService _ws;

  DriverNotificationHandler(this._ws);

  void register() {
    // Новый заказ — показать оффер
    _ws.on('TRIP_OFFER', (payload) {
      final passenger = payload.passengerProfile;
      showTripOffer(
        tripId:         payload.tripId!,
        message:        payload.message,
        passengerName:  passenger?.fullName,
        passengerRating: passenger?.averageRating,
      );
    });

    // Оффер истёк
    _ws.on('TRIP_OFFER_EXPIRED', (payload) {
      dismissTripOffer();
    });

    // Поездка отменена пассажиром
    _ws.on('TRIP_CANCELLED', (payload) {
      showNotification(payload.message);
      navigateToOnlineScreen();
    });

    // Выплата
    _ws.on('PAYMENT_SUCCEEDED', (payload) {
      showNotification(payload.message);
    });
  }
}
```

## !ПРИМЕР!

## Инициализация в приложении


```
// lib/main.dart или в AuthBloc после логина

final notificationService = NotificationService();

// После успешного логина:
void onLoginSuccess(String token, String userType) {
  notificationService.connect(
    token: token,
    userType: userType,  // 'DRIVER' или 'PASSENGER'
  );

  if (userType == 'DRIVER') {
    DriverNotificationHandler(notificationService).register();
  } else {
    PassengerNotificationHandler(notificationService).register();
  }
}

// При логауте:
void onLogout() {
  notificationService.disconnect();
}
```


## Полный список событий

| eventType            | Кому                | driverProfile | passengerProfile |
| -------------------- | ------------------- | ------------- | ---------------- |
| `TRIP_OFFER`         | Водитель            | null          | ✅                |
| `TRIP_OFFER_EXPIRED` | Водитель            | null          | null             |
| `DRIVER_ASSIGNED`    | Пассажир            | ✅             | null             |
| `TRIP_STARTED`       | Пассажир            | null          | null             |
| `TRIP_COMPLETED`     | Пассажир            | null          | null             |
| `TRIP_CANCELLED`     | Пассажир + Водитель | null          | null             |
| `PAYMENT_SUCCEEDED`  | Пассажир + Водитель | null          | null             |
| `PAYMENT_FAILED`     | Пассажир            | null          | null             |
| `REFUND_SUCCEEDED`   | Пассажир            | null          | null             |
| `USER_REGISTERED`    | Пассажир            | null          | null             |
## Важные условности

```
JWT токен → передаётся в query param (?token=...)
userType  → передаётся в STOMP CONNECT заголовке
           'DRIVER'    — водитель (может слать локацию)
           'PASSENGER' — пассажир (только получает)

Локация (только для водителя) — отдельный STOMP SEND:
  destination: /app/driver/location
  body: { latitude, longitude, vehicleClass }
  
Все уведомления приходят на:
  destination: /user/queue/notifications
```
## Поток взаимодействия с сокетом
## 1) HTTP логин, получение токена

**Что происходит:** перед подключением к WebSocket нужен JWT токен. Без него сервер отклонит соединение ещё на этапе handshake — это проверяет `JwtHandshakeInterceptor`.

```
final response = await http.post(
  Uri.parse('$apiUrl/auth/login'),
  body: jsonEncode({'email': email, 'password': password}),
);

final token = response.json['accessTokenDto']['token'];
final userType = 'PASSENGER'; // или 'DRIVER' — определяешь сам по контексту
```

## 2) Открытие WebSocket соединения

**Что происходит:** Flutter открывает TCP соединение с сервером и делает HTTP Upgrade до WebSocket. В этот момент сервер проверяет JWT из query param через `JwtHandshakeInterceptor` — если токен невалидный или отсутствует, соединение закрывается сразу.


```
Flutter                          Backend
  │                                │
  │  GET /ws/notifications/        │
  │  websocket?token=JWT_TOKEN     │
  │ ──────────────────────────────►│
  │                                │  JwtHandshakeInterceptor
  │                                │  проверяет токен
  │                                │  кладёт userId, role
  │                                │  в session attributes
  │  101 Switching Protocols       │
  │ ◄──────────────────────────────│
```


```
_stompClient = StompClient(
  config: StompConfig(
    url: '$wsUrl?token=$token',  // ← токен в URL
    ...
  ),
);
_stompClient!.activate();  // ← открывает соединение
```

## 3) STOMP CONNECT

**Что происходит:** WebSocket — это просто канал передачи данных. STOMP — это протокол поверх него, который добавляет понятия команд (CONNECT, SUBSCRIBE, SEND), заголовков и очередей. После открытия WebSocket клиент отправляет STOMP CONNECT фрейм. Сервер проверяет его через `StompAuthChannelInterceptor` — достаёт userId из сессии, читает userType из заголовка, создаёт объект аутентификации.


```
Flutter                          Backend
  │                                │
  │  STOMP CONNECT                 │
  │  userType: DRIVER              │
  │  heart-beat: 10000,10000       │
  │ ──────────────────────────────►│
  │                                │  StompAuthChannelInterceptor
  │                                │  читает userId из сессии
  │                                │  читает userType из заголовка
  │                                │  создаёт Authentication объект
  │                                │
  │                                │  StompSessionEventListener
  │                                │  регистрирует сессию в Registry
  │  STOMP CONNECTED               │
  │ ◄──────────────────────────────│
```


```
stompConnectHeaders: {
  'userType': userType,         // ← DRIVER или PASSENGER
  'heart-beat': '10000,10000',  // ← keepalive каждые 10 сек
},
```


## 4) Подписка на очередь

**Что происходит:** клиент говорит серверу "я хочу получать сообщения из этой очереди". `/user/queue/notifications` — это персональная очередь, Spring автоматически привязывает её к конкретному userId из Authentication объекта. То есть сообщения из этой очереди получит **только** этот пользователь.


```
Flutter                          Backend
  │                                │
  │  STOMP SUBSCRIBE               │
  │  destination:                  │
  │  /user/queue/notifications     │
  │ ──────────────────────────────►│
  │                                │  Spring связывает очередь
  │                                │  с userId=602
  │                                │  (из Authentication)
```


```
void _onConnect(StompFrame frame) {
  _stompClient!.subscribe(
    destination: '/user/queue/notifications',  // ← персональная очередь
    callback: _onMessage,
  );
}
```


## 5) Получение уведомлений

**Что происходит:** когда в системе происходит событие (водитель назначен, поездка началась и т.д.) — TripService публикует сообщение в RabbitMQ. NotificationService получает его, сохраняет в БД, обогащает профилями (если нужно) и отправляет по WebSocket конкретному пользователю через `convertAndSendToUser`.


```
TripService                  RabbitMQ            NotificationService        Flutter
    │                           │                       │                      │
    │  publishDriverAssigned()  │                       │                      │
    │ ─────────────────────────►│                       │                      │
    │                           │  consume()            │                      │
    │                           │ ─────────────────────►│                      │
    │                           │                       │  save() в БД         │
    │                           │                       │  getDriverProfile()  │
    │                           │                       │  sendToUser()        │
    │                           │                       │ ────────────────────►│
    │                           │                       │                      │  _onMessage()
```


```
void _onMessage(StompFrame frame) {
  final json    = jsonDecode(frame.body!);
  final payload = NotificationPayload.fromJson(json);

  // Пример payload для DRIVER_ASSIGNED:
  // {
  //   "notificationId": 254,
  //   "tripId": 653,
  //   "eventType": "DRIVER_ASSIGNED",
  //   "message": "Водитель найден и едет к вам!",
  //   "createdAt": "2026-05-05T05:17:57.772653Z",
  //   "driverProfile": {
  //     "accountId": 579,
  //     "firstName": "Сергей",
  //     "lastName": "Сидоров",
  //     "photoUrl": "https://cdn.example.com/driver.jpg",
  //     "averageRating": 0.00
  //   },
  //   "passengerProfile": null
  // }

  final handler = _handlers[payload.eventType];
  handler?.call(payload);
}
```

## 6) Heartbeat (keepalive)

**Что происходит:** WebSocket соединение может быть закрыто сетевым оборудованием (NAT, прокси) если долго нет трафика. Heartbeat — это пустые пакеты которые клиент и сервер периодически шлют друг другу чтобы соединение не закрылось. `10000,10000` означает "я буду слать каждые 10 сек и ожидаю от тебя каждые 10 сек".


```
heartbeatIncoming: const Duration(seconds: 10),
heartbeatOutgoing: const Duration(seconds: 10),
```

## 7) Отправка локации (только для водителя)

**Что происходит:** водитель не только получает уведомления, но и **отправляет** свою локацию серверу через тот же WebSocket. Это STOMP SEND на destination `/app/driver/location`. Сервер проверяет через `StompAuthChannelInterceptor` что отправитель — именно DRIVER (иначе блокирует).


```
Flutter (DRIVER)                 Backend
  │                                │
  │  STOMP SEND                    │
  │  destination:                  │
  │  /app/driver/location          │
  │  { lat, lng, vehicleClass }    │
  │ ──────────────────────────────►│
  │                                │  StompAuthChannelInterceptor
  │                                │  проверяет userType=DRIVER
  │                                │
  │                                │  DriverLocationController
  │                                │  обновляет локацию в Redis
```


```
// Только для водителя — периодически отправлять локацию
Timer.periodic(Duration(seconds: 5), (_) {
  if (_stompClient?.connected == true) {
    _stompClient!.send(
      destination: '/app/driver/location',
      body: jsonEncode({
        'latitude':     currentLat,
        'longitude':    currentLng,
        'vehicleClass': 'COMFORT',
      }),
    );
  }
});
```

## 8) Отключение

**Что происходит:** при логауте или закрытии приложения клиент закрывает WebSocket. Сервер получает `SessionDisconnectEvent`, находит сессию в `WebSocketSessionRegistry`. Если это был водитель — автоматически переводит его в статус OFFLINE через HTTP вызов в UserService.


```
Flutter                          Backend
  │                                │
  │  WebSocket close               │
  │ ──────────────────────────────►│
  │                                │  SessionDisconnectEvent
  │                                │
  │                                │  isDriver? → setDriverOffline()
  │                                │  unregister() из Registry
```


```
void onLogout() {
  _stompClient?.deactivate();  // ← корректное закрытие
}
```
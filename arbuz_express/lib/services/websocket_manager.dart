import 'dart:async';
import 'dart:convert';
import 'package:flutter/material.dart';
import 'package:stomp_dart_client/stomp_dart_client.dart';
import '../config/api_config.dart';
import 'token_storage.dart';

class WebSocketManager {
  static final WebSocketManager _instance = WebSocketManager._internal();
  factory WebSocketManager() => _instance;
  WebSocketManager._internal();

  StompClient? _client;
  String? _role;
  Timer? _locationTimer;
  Timer? _heartbeatTimer;

  void start(String role) {
    if (_client != null && _client!.connected) return;

    _role = role;
    final String wsUrl = '${ApiConfig.wsUrl}?token=${TokenStorage.accessToken}';

    _client = StompClient(
      config: StompConfig(
        url: wsUrl,
        onConnect: (frame) => _onConnected(),
        onWebSocketError: (dynamic error) => debugPrint('[WS] Error: $error'),
        stompConnectHeaders: {
          'accept-version': '1.1',
          'heart-beat': '10000,10000',
          'host': ApiConfig.host,
          'userType': role.toUpperCase(),
        },
      ),
    );
    _client!.activate();
  }

  void _onConnected() {
    _subscribe('/user/queue/notifications');

    if (_role == 'driver') {
      _startLocationLoop();
    } else if (_role == 'passenger') {
      _startHeartbeatLoop();
    }
  }

  void _subscribe(String destination) {
    if (_client != null && _client!.connected) {
      _client!.subscribe(
        destination: destination,
        callback: (frame) {
          debugPrint('[WS] Message: ${frame.body}');
        },
      );
    }
  }

  void _startLocationLoop() {
    _locationTimer?.cancel();
    _locationTimer = Timer.periodic(const Duration(seconds: 5), (_) {
      _sendLocationUpdate();
    });
  }

  void _sendLocationUpdate() {
    final payload = jsonEncode({
      'latitude': 55.755864,
      'longitude': 37.617617,
      'vehicleClass': 'COMFORT',
    });
    send('/app/driver/location', payload);
  }

  void _startHeartbeatLoop() {
    _heartbeatTimer?.cancel();
    _heartbeatTimer = Timer.periodic(const Duration(seconds: 10), (_) {
      _sendHeartbeat();
    });
  }

  void _sendHeartbeat() {
    if (_client != null && _client!.connected) {
      _client!.send(destination: '/app/heartbeat', body: '');
    }
  }

  void stopLocationUpdates() {
    _locationTimer?.cancel();
    _locationTimer = null;
  }

  void startLocationUpdates() {
    if (_role == 'driver') {
      _startLocationLoop();
    }
  }

  void send(String destination, String body) {
    if (_client != null && _client!.connected) {
      _client!.send(destination: destination, body: body);
    }
  }

  void stop() {
    _locationTimer?.cancel();
    _heartbeatTimer?.cancel();
    _client?.deactivate();
    _client = null;
  }
}

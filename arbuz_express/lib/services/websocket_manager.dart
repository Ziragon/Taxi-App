import 'dart:async';
import 'dart:convert';
import 'package:flutter/material.dart';
import 'package:stomp_dart_client/stomp_dart_client.dart';
import 'package:geolocator/geolocator.dart';
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
  double _currentLat = 55.755864; // Default: Red Square
  double _currentLng = 37.617617; // Default: Red Square

  void Function(Map<String, dynamic>)? onNotification;

  bool get isConnected => _client?.connected ?? false;

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
      // Send location immediately on connect, then start periodic updates
      _sendLocationUpdate();
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
          final body = _parseNotificationBody(frame.body);
          if (body != null) {
            onNotification?.call(body);
          }
        },
      );
    }
  }

  Map<String, dynamic>? _parseNotificationBody(String? body) {
    if (body == null || body.isEmpty) return null;
    final cleaned = body.replaceAll('\x00', '').trim();
    if (cleaned.isEmpty) return null;
    try {
      return jsonDecode(cleaned) as Map<String, dynamic>;
    } catch (_) {
      return null;
    }
  }

  void _startLocationLoop() {
    _locationTimer?.cancel();
    _locationTimer = Timer.periodic(const Duration(seconds: 5), (_) {
      _sendLocationUpdate();
    });
  }

  void _sendLocationUpdate() async {
    try {
      // Get current location
      Position position = await Geolocator.getCurrentPosition(
        desiredAccuracy: LocationAccuracy.high,
        timeLimit: const Duration(seconds: 10),
      );

      _currentLat = position.latitude;
      _currentLng = position.longitude;

      // Validate that coordinates are not null before sending
      if (_currentLat == null || _currentLng == null) {
        debugPrint('[WS] Error: Location coordinates are null, skipping send');
        return;
      }

      final payload = jsonEncode({
        'longitude': _currentLng,
        'latitude': _currentLat,
        'vehicleClass': 'COMFORT',
      });
      final headers = {
        'content-type': 'application/json',
        'content-length': utf8.encode(payload).length.toString(),
      };

      debugPrint(
        '[WS] Sending location via STOMP: longitude=${_currentLng.toStringAsFixed(6)}, latitude=${_currentLat.toStringAsFixed(6)}, vehicleClass=COMFORT',
      );
      debugPrint('[WS] Full payload: $payload');
      debugPrint('[WS] Headers: $headers');
      send('/app/driver/location', payload, headers: headers);
    } catch (e) {
      debugPrint('[WS] Error getting location: $e, will retry in next cycle');
      // Don't send incomplete data - wait for next attempt
    }
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

  void send(String destination, String body, {Map<String, String>? headers}) {
    if (_client == null) {
      debugPrint(
        '[WS] Error: STOMP client is null, cannot send to $destination',
      );
      return;
    }
    if (!_client!.connected) {
      debugPrint(
        '[WS] Error: STOMP not connected, cannot send to $destination',
      );
      return;
    }
    debugPrint('[WS] Sending to $destination');
    debugPrint('[WS] Payload: $body');
    if (headers != null) {
      debugPrint('[WS] Headers: $headers');
    }
    _client!.send(destination: destination, body: body, headers: headers);
  }

  void stop() {
    _locationTimer?.cancel();
    _heartbeatTimer?.cancel();
    _client?.deactivate();
    _client = null;
  }
}

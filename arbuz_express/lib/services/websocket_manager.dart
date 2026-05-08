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

  void _sendLocationUpdate() async {
    try {
      // Get current location
      Position position = await Geolocator.getCurrentPosition(
        desiredAccuracy: LocationAccuracy.high,
        timeLimit: const Duration(seconds: 10),
      );

      _currentLat = position.latitude;
      _currentLng = position.longitude;

      final payload = jsonEncode({
        'latitude': _currentLat,
        'longitude': _currentLng,
        'vehicleClass': 'COMFORT',
      });

      debugPrint(
        '[WS] Sending real location: lat=${_currentLat.toStringAsFixed(6)}, lng=${_currentLng.toStringAsFixed(6)}',
      );
      send('/app/driver/location', payload);
    } catch (e) {
      debugPrint('[WS] Error getting location: $e, using last known position');
      // Send last known position if we can't get new one
      final payload = jsonEncode({
        'latitude': _currentLat,
        'longitude': _currentLng,
        'vehicleClass': 'COMFORT',
      });
      send('/app/driver/location', payload);
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

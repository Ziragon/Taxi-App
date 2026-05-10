import 'dart:async';
import 'dart:convert';
import 'websocket_service.dart';

class DriverWebSocketService extends WebSocketService {
  Timer? _locationTimer;
  double _latitude = 55.755864;
  double _longitude = 37.617617;
  String _vehicleClass = 'COMFORT';

  @override
  String get userType => 'DRIVER';

  void updateLocation(double lat, double lng) {
    _latitude = lat;
    _longitude = lng;
  }

  void updateVehicleClass(String vehicleClass) {
    _vehicleClass = vehicleClass;
  }

  @override
  void _onConnected() {
    _startLocationLoop();
  }

  void _startLocationLoop() {
    _locationTimer?.cancel();
    _locationTimer = Timer.periodic(const Duration(seconds: 5), (_) {
      _sendLocation();
    });
  }

  void _sendLocation() {
    final payload = jsonEncode({
      'longitude': _longitude,
      'latitude': _latitude,
      'vehicleClass': _vehicleClass,
    });
    sendFrame('SEND', {
      'destination': '/app/driver/location',
      'content-type': 'application/json',
      'content-length': utf8.encode(payload).length.toString(),
    }, body: payload);
  }

  @override
  void disconnect() {
    _locationTimer?.cancel();
    _locationTimer = null;
    super.disconnect();
  }
}

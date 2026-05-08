import 'dart:async';
import 'websocket_service.dart';

class PassengerWebSocketService extends WebSocketService {
  Timer? _heartbeatTimer;

  @override
  String get userType => 'PASSENGER';

  @override
  void _onConnected() {
    _startHeartbeatLoop();
  }

  void _startHeartbeatLoop() {
    _heartbeatTimer?.cancel();
    _heartbeatTimer = Timer.periodic(const Duration(seconds: 10), (_) {
      sendHeartbeat();
    });
  }

  @override
  void disconnect() {
    _heartbeatTimer?.cancel();
    _heartbeatTimer = null;
    super.disconnect();
  }
}

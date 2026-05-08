import 'package:web_socket_channel/web_socket_channel.dart';
import 'package:web_socket_channel/io.dart';
import 'stomp_frame.dart';
import 'token_storage.dart';
import '../config/api_config.dart';

abstract class WebSocketService {
  WebSocketChannel? _channel;
  bool _connected = false;
  String? _subscriptionId;
  Function(String)? onNotification;

  String get userType;
  String get wsUrl => '${ApiConfig.wsUrl}?token=${TokenStorage.accessToken}';

  Future<void> connect() async {
    if (_channel != null) return;
    try {
      _channel = IOWebSocketChannel.connect(Uri.parse(wsUrl));
      _channel!.stream.listen(
        (dynamic message) {
          _handleMessage(message.toString());
        },
        onDone: _handleClose,
        onError: _handleError,
      );
      _sendConnect();
    } catch (e) {
      _handleClose();
    }
  }

  void _sendConnect() {
    final frame = StompFrame.build('CONNECT', {
      'accept-version': '1.1',
      'heart-beat': '10000,10000',
      'host': ApiConfig.host,
      'userType': userType,
    });
    _channel!.sink.add(frame);
  }

  void _handleMessage(String message) {
    if (message == '\n' || message == '\r\n') return;

    if (StompFrame.isConnectedFrame(message)) {
      _connected = true;
      _sendSubscribe();
      _onConnected();
    } else if (StompFrame.isMessageFrame(message)) {
      final notification = _extractNotificationBody(message);
      if (notification != null && onNotification != null) {
        onNotification!(notification);
      }
    }
  }

  void _sendSubscribe() {
    _subscriptionId = 'sub-${DateTime.now().millisecondsSinceEpoch}';
    final frame = StompFrame.build('SUBSCRIBE', {
      'id': _subscriptionId!,
      'destination': '/user/queue/notifications',
    });
    _channel!.sink.add(frame);
  }

  String? _extractNotificationBody(String message) {
    final parts = message.split('\n\n');
    if (parts.length >= 2) {
      final bodyWithNull = parts[1];
      return bodyWithNull.replaceAll('\x00', '');
    }
    return null;
  }

  void _handleClose() {
    _connected = false;
    _channel = null;
  }

  void _handleError(dynamic error) {
    _handleClose();
  }

  void sendFrame(
    String command,
    Map<String, String> headers, {
    String body = '',
  }) {
    if (_channel == null) return;
    final frame = StompFrame.build(command, headers, body: body);
    _channel!.sink.add(frame);
  }

  void disconnect() {
    if (_channel != null) {
      _channel!.sink.close();
      _channel = null;
      _connected = false;
    }
  }

  bool get isConnected => _connected;
  void _onConnected();

  void sendHeartbeat() {
    if (_connected && _channel != null) {
      _channel!.sink.add('\n');
    }
  }
}

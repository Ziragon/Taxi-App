class StompFrame {
  static String build(
    String command,
    Map<String, String> headers, {
    String body = '',
  }) {
    final buffer = StringBuffer();
    buffer.write('$command\n');
    headers.forEach((key, value) {
      buffer.write('$key:$value\n');
    });
    buffer.write('\n');
    buffer.write(body);
    buffer.write('\x00');
    return buffer.toString();
  }

  static bool isConnectedFrame(String message) {
    return message.startsWith('CONNECTED');
  }

  static bool isMessageFrame(String message) {
    return message.startsWith('MESSAGE');
  }
}

import 'dart:convert';
import 'package:http/http.dart' as http;
import '../config/api_config.dart';
import '../services/token_storage.dart';

class UseAuth {
  Future<AuthResult> register({
    required String email,
    required String phone,
    required String password,
  }) async {
    try {
      final response = await http.post(
        Uri.parse(ApiConfig.authRegister),
        headers: {'Content-Type': 'application/json'},
        body: jsonEncode({
          "email": email,
          "phone": phone,
          "password": password,
        }),
      );

      if (response.statusCode == 200 || response.statusCode == 201) {
        final responseData = jsonDecode(response.body);
        if (responseData['accessTokenDto'] != null &&
            responseData['accessTokenDto']['token'] != null) {
          TokenStorage.accessToken = responseData['accessTokenDto']['token'];
        }
        return AuthResult(success: true);
      } else {
        return AuthResult(
          success: false,
          error: 'Ошибка регистрации: ${response.statusCode}',
        );
      }
    } catch (e) {
      return AuthResult(success: false, error: 'Ошибка сети: $e');
    }
  }

  Future<AuthResult> login({
    required String email,
    required String password,
  }) async {
    try {
      final response = await http.post(
        Uri.parse(ApiConfig.authLogin),
        headers: {'Content-Type': 'application/json'},
        body: jsonEncode({"email": email, "password": password}),
      );

      if (response.statusCode == 200 || response.statusCode == 201) {
        final responseData = jsonDecode(response.body);

        if (responseData['accessTokenDto'] != null &&
            responseData['accessTokenDto']['token'] != null) {
          TokenStorage.accessToken = responseData['accessTokenDto']['token'];
        }

        final bool hasProfile = responseData['passengerProfile'] != null;

        return AuthResult(success: true, hasProfile: hasProfile);
      } else {
        return AuthResult(
          success: false,
          error: 'Ошибка авторизации: ${response.statusCode}',
        );
      }
    } catch (e) {
      return AuthResult(success: false, error: 'Ошибка сети: $e');
    }
  }
}

class AuthResult {
  final bool success;
  final String? error;
  final bool hasProfile;

  AuthResult({required this.success, this.error, this.hasProfile = false});
}

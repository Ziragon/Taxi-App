import 'dart:convert';
import 'package:http/http.dart' as http;
import '../config/api_config.dart';
import '../services/token_storage.dart';

class AuthResult {
  final bool success;
  final String? error;
  final bool hasProfile;

  AuthResult({required this.success, this.error, this.hasProfile = false});
}

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

        if (responseData['driverProfile'] != null) {
          TokenStorage.userRole = 'driver';
        } else if (responseData['passengerProfile'] != null) {
          TokenStorage.userRole = 'passenger';
        }

        return AuthResult(
          success: true,
          hasProfile: TokenStorage.userRole != null,
        );
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

  Future<AuthResult> logout() async {
    try {
      final response = await http.post(
        Uri.parse(ApiConfig.authLogout),
        headers: TokenStorage.getAuthHeaders(),
      );

      if (response.statusCode == 204 || response.statusCode == 200) {
        TokenStorage.accessToken = null;
        TokenStorage.userRole = null;
        return AuthResult(success: true);
      } else {
        return AuthResult(
          success: false,
          error: 'Ошибка выхода: ${response.statusCode}',
        );
      }
    } catch (e) {
      return AuthResult(success: false, error: 'Ошибка сети: $e');
    }
  }
}

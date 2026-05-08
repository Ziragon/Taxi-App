import 'package:flutter/material.dart';
import 'package:http/http.dart' as http;
import 'dart:convert';
import '../config/api_config.dart';
import '../services/token_storage.dart';

class UseDriverStatus {
  Future<Map<String, dynamic>> setOnline() async {
    try {
      debugPrint('=== DRIVER SET ONLINE ===');
      debugPrint('URL: ${ApiConfig.driverOnline}');
      debugPrint('Token: ${TokenStorage.accessToken}');
      debugPrint('Headers: ${TokenStorage.getAuthHeaders()}');

      final response = await http.put(
        Uri.parse(ApiConfig.driverOnline),
        headers: TokenStorage.getAuthHeaders(),
      );

      debugPrint('Response status: ${response.statusCode}');
      debugPrint('Response body: ${response.body}');

      if (response.statusCode == 200 || response.statusCode == 204) {
        return {'success': true, 'message': 'Вы успешно вышли в онлайн'};
      } else if (response.statusCode == 403) {
        try {
          final body = jsonDecode(response.body);
          final message = body['message'] ?? 'Ошибка доступа';
          return {'success': false, 'message': message, 'code': 'FORBIDDEN'};
        } catch (e) {
          return {
            'success': false,
            'message': 'Ошибка доступа. Проверьте данные профиля',
            'code': 'FORBIDDEN'
          };
        }
      } else {
        return {
          'success': false,
          'message': 'Ошибка сервера. Попробуйте позже',
          'code': 'ERROR'
        };
      }
    } catch (e) {
      debugPrint('Ошибка при выходе в онлайн: $e');
      return {
        'success': false,
        'message': 'Ошибка соединения',
        'code': 'NETWORK'
      };
    }
  }

  Future<Map<String, dynamic>> setOffline() async {
    try {
      debugPrint('=== DRIVER SET OFFLINE ===');
      debugPrint('URL: ${ApiConfig.driverOffline}');
      debugPrint('Token: ${TokenStorage.accessToken}');

      final response = await http.put(
        Uri.parse(ApiConfig.driverOffline),
        headers: TokenStorage.getAuthHeaders(),
      );

      debugPrint('Response status: ${response.statusCode}');
      debugPrint('Response body: ${response.body}');

      if (response.statusCode == 200 || response.statusCode == 204) {
        return {'success': true, 'message': 'Вы ушли в офлайн'};
      } else if (response.statusCode == 403) {
        try {
          final body = jsonDecode(response.body);
          final message = body['message'] ?? 'Ошибка доступа';
          return {'success': false, 'message': message, 'code': 'FORBIDDEN'};
        } catch (e) {
          return {
            'success': false,
            'message': 'Ошибка доступа',
            'code': 'FORBIDDEN'
          };
        }
      } else {
        return {
          'success': false,
          'message': 'Ошибка сервера. Попробуйте позже',
          'code': 'ERROR'
        };
      }
    } catch (e) {
      debugPrint('Ошибка при уходе в офлайн: $e');
      return {
        'success': false,
        'message': 'Ошибка соединения',
        'code': 'NETWORK'
      };
    }
  }
}

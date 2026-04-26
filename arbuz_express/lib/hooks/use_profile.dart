import 'dart:convert';
import 'package:http/http.dart' as http;
import '../config/api_config.dart';
import '../services/token_storage.dart';

class UseProfile {
  Future<ProfileResult> createDriverProfile({
    required String firstName,
    required String lastName,
    required String licenseNumber,
    String photoUrl = 'https://cdn.example.com/driver.jpg',
  }) async {
    try {
      final response = await http.post(
        Uri.parse(ApiConfig.profilesDriver),
        headers: TokenStorage.getAuthHeaders(),
        body: jsonEncode({
          "firstName": firstName,
          "lastName": lastName,
          "licenseNumber": licenseNumber,
          "photoUrl": photoUrl,
        }),
      );

      if (response.statusCode == 200 || response.statusCode == 201) {
        return ProfileResult(success: true);
      } else {
        return ProfileResult(
          success: false,
          error: 'Ошибка создания профиля водителя: ${response.statusCode}',
        );
      }
    } catch (e) {
      return ProfileResult(success: false, error: 'Ошибка сети: $e');
    }
  }

  Future<ProfileResult> createPassengerProfile({
    required String firstName,
    required String lastName,
    String photoUrl = 'https://cdn.example.com/avatar.jpg',
  }) async {
    try {
      final response = await http.post(
        Uri.parse(ApiConfig.profilesPassenger),
        headers: TokenStorage.getAuthHeaders(),
        body: jsonEncode({
          "firstName": firstName,
          "lastName": lastName,
          "photoUrl": photoUrl,
        }),
      );

      if (response.statusCode == 200 || response.statusCode == 201) {
        return ProfileResult(success: true);
      } else {
        return ProfileResult(
          success: false,
          error: 'Ошибка создания профиля пассажира: ${response.statusCode}',
        );
      }
    } catch (e) {
      return ProfileResult(success: false, error: 'Ошибка сети: $e');
    }
  }
}

class ProfileResult {
  final bool success;
  final String? error;

  ProfileResult({required this.success, this.error});
}

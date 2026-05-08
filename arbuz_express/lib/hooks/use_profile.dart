import 'dart:convert';
import 'package:http/http.dart' as http;
import '../config/api_config.dart';
import '../services/token_storage.dart';
import '../services/websocket_manager.dart';

class UserProfileData {
  final String firstName;
  final String lastName;
  final String photoUrl;
  final double averageRating;
  final int totalTrips;
  final String? licenseNumber;

  UserProfileData({
    required this.firstName,
    required this.lastName,
    required this.photoUrl,
    required this.averageRating,
    required this.totalTrips,
    this.licenseNumber,
  });

  factory UserProfileData.fromJson(Map<String, dynamic> json) {
    return UserProfileData(
      firstName: json['firstName'] ?? '',
      lastName: json['lastName'] ?? '',
      photoUrl: json['photoUrl'] ?? '',
      averageRating: (json['averageRating'] ?? 0).toDouble(),
      totalTrips: json['totalTrips'] ?? 0,
      licenseNumber: json['licenseNumber'],
    );
  }
}

class ProfileResult {
  final bool success;
  final String? error;
  final UserProfileData? data;

  ProfileResult({required this.success, this.error, this.data});
}

class UseProfile {
  Future<UserProfileData?> getCurrentProfile() async {
    final String url = TokenStorage.userRole == 'driver'
        ? ApiConfig.profilesDriver
        : ApiConfig.profilesPassenger;

    try {
      final response = await http.get(
        Uri.parse(url),
        headers: TokenStorage.getAuthHeaders(),
      );

      if (response.statusCode == 200) {
        return UserProfileData.fromJson(jsonDecode(response.body));
      }
    } catch (e) {}
    return null;
  }

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
        TokenStorage.userRole = 'driver';
        WebSocketManager().start('driver');
        final data = UserProfileData.fromJson(jsonDecode(response.body));
        return ProfileResult(success: true, data: data);
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
        TokenStorage.userRole = 'passenger';
        WebSocketManager().start('passenger');
        final data = UserProfileData.fromJson(jsonDecode(response.body));
        return ProfileResult(success: true, data: data);
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

  Future<ProfileResult> updateDriverProfile({
    required String firstName,
    required String lastName,
    required String licenseNumber,
    required String photoUrl,
  }) async {
    try {
      final response = await http.put(
        Uri.parse(ApiConfig.profilesDriver),
        headers: TokenStorage.getAuthHeaders(),
        body: jsonEncode({
          "firstName": firstName,
          "lastName": lastName,
          "licenseNumber": licenseNumber,
          "photoUrl": photoUrl,
        }),
      );

      if (response.statusCode == 200) {
        final data = UserProfileData.fromJson(jsonDecode(response.body));
        return ProfileResult(success: true, data: data);
      } else {
        return ProfileResult(
          success: false,
          error: 'Ошибка обновления профиля водителя: ${response.statusCode}',
        );
      }
    } catch (e) {
      return ProfileResult(success: false, error: 'Ошибка сети: $e');
    }
  }

  Future<ProfileResult> updatePassengerProfile({
    required String firstName,
    required String lastName,
    required String photoUrl,
  }) async {
    try {
      final response = await http.put(
        Uri.parse(ApiConfig.profilesPassenger),
        headers: TokenStorage.getAuthHeaders(),
        body: jsonEncode({
          "firstName": firstName,
          "lastName": lastName,
          "photoUrl": photoUrl,
        }),
      );

      if (response.statusCode == 200) {
        final data = UserProfileData.fromJson(jsonDecode(response.body));
        return ProfileResult(success: true, data: data);
      } else {
        return ProfileResult(
          success: false,
          error: 'Ошибка обновления профиля пассажира: ${response.statusCode}',
        );
      }
    } catch (e) {
      return ProfileResult(success: false, error: 'Ошибка сети: $e');
    }
  }
}

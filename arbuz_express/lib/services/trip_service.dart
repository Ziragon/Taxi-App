import 'package:http/http.dart' as http;
import 'dart:convert';
import 'package:arbuz_express/config/api_config.dart';
import 'package:arbuz_express/models/trip_models.dart';
import 'package:arbuz_express/services/token_storage.dart';

class TripService {
  static const String _baseUrl = ApiConfig.baseUrl;

  static Future<TripCalculationResponse> calculateTrip(
    TripCalculationRequest request,
  ) async {
    try {
      final headers = TokenStorage.getAuthHeaders();
      headers['Content-Type'] = 'application/json';

      final response = await http.post(
        Uri.parse('$_baseUrl/trips'),
        headers: headers,
        body: jsonEncode(request.toJson()),
      );

      if (response.statusCode == 200) {
        return TripCalculationResponse.fromJson(
          jsonDecode(response.body) as Map<String, dynamic>,
        );
      } else if (response.statusCode == 401) {
        throw UnauthorizedException('Unauthorized');
      } else {
        throw ServerException(
          'Failed to calculate trip: ${response.statusCode}',
        );
      }
    } on http.ClientException catch (e) {
      throw NetworkException('Network error: $e');
    } catch (e) {
      rethrow;
    }
  }

  static Future<void> startSearching({required String vehicleClass}) async {
    final response = await http.post(
      Uri.parse('$_baseUrl/trips/start-search?vehicleClass=$vehicleClass'),
      headers: TokenStorage.getAuthHeaders(),
    );

    if (response.statusCode != 200 && response.statusCode != 201) {
      throw ServerException('Failed to start search');
    }
  }

  static Future<TripDetails> getTripDetails(int tripId) async {
    final response = await http.get(
      Uri.parse('$_baseUrl/trips/$tripId'),
      headers: TokenStorage.getAuthHeaders(),
    );

    if (response.statusCode == 200) {
      return TripDetails.fromJson(
        jsonDecode(response.body) as Map<String, dynamic>,
      );
    }

    throw ServerException('Failed to get trip details: ${response.statusCode}');
  }

  static Future<void> cancelTrip(int tripId) async {
    final response = await http.post(
      Uri.parse('$_baseUrl/trips/$tripId/cancel'),
      headers: TokenStorage.getAuthHeaders(),
    );

    if (response.statusCode != 200) {
      throw ServerException('Failed to cancel trip');
    }
  }

  static Future<TripDetails> getActiveTrip() async {
    final response = await http.get(
      Uri.parse('$_baseUrl/trips/active'),
      headers: TokenStorage.getAuthHeaders(),
    );

    if (response.statusCode == 200) {
      return TripDetails.fromJson(
        jsonDecode(response.body) as Map<String, dynamic>,
      );
    }

    throw ServerException('Failed to get active trip: ${response.statusCode}');
  }

  static Future<void> acceptTrip(int tripId, double lat, double lng) async {
    final headers = TokenStorage.getAuthHeaders();
    headers['Content-Type'] = 'application/json';

    final response = await http.post(
      Uri.parse('$_baseUrl/trips/$tripId/accept'),
      headers: headers,
      body: jsonEncode({'latitude': lat, 'longitude': lng}),
    );

    if (response.statusCode != 200) {
      throw ServerException('Failed to accept trip');
    }
  }

  static Future<void> rejectTrip(int tripId) async {
    final response = await http.post(
      Uri.parse('$_baseUrl/trips/$tripId/reject'),
      headers: TokenStorage.getAuthHeaders(),
    );

    if (response.statusCode != 200) {
      throw ServerException('Failed to reject trip');
    }
  }

  static Future<void> startTrip(int tripId) async {
    final response = await http.post(
      Uri.parse('$_baseUrl/trips/$tripId/start'),
      headers: TokenStorage.getAuthHeaders(),
    );

    if (response.statusCode != 200) {
      throw ServerException('Failed to start trip');
    }
  }

  static Future<void> completeTrip(int tripId) async {
    final response = await http.post(
      Uri.parse('$_baseUrl/trips/$tripId/complete'),
      headers: TokenStorage.getAuthHeaders(),
    );

    if (response.statusCode != 200) {
      throw ServerException('Failed to complete trip: ${response.statusCode}');
    }
  }

  static Future<TripDetails> getDriverActiveTrip(double lat, double lng) async {
    final request = http.Request(
      'GET',
      Uri.parse('$_baseUrl/trips/driver-active'),
    );
    request.headers.addAll(TokenStorage.getAuthHeaders());
    request.headers['Content-Type'] = 'application/json';
    request.body = jsonEncode({'latitude': lat, 'longitude': lng});

    final streamedResponse = await request.send();
    final response = await http.Response.fromStream(streamedResponse);

    if (response.statusCode == 200) {
      return TripDetails.fromJson(jsonDecode(response.body));
    } else {
      throw ServerException('Failed to get active trip');
    }
  }
}

class ServerException implements Exception {
  final String message;
  ServerException(this.message);
  @override
  String toString() => message;
}

class NetworkException implements Exception {
  final String message;
  NetworkException(this.message);
  @override
  String toString() => message;
}

class UnauthorizedException implements Exception {
  final String message;
  UnauthorizedException(this.message);
  @override
  String toString() => message;
}

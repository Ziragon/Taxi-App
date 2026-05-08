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

  static Future<TripCalculationResponse> startSearching(
    int tripId,
    String vehicleClass,
  ) async {
    try {
      final headers = TokenStorage.getAuthHeaders();
      headers['Content-Type'] = 'application/json';

      final response = await http.post(
        Uri.parse('$_baseUrl/trips/start-search?vehicleClass=$vehicleClass'),
        headers: headers,
        body: jsonEncode({'tripId': tripId}),
      );

      if (response.statusCode == 200) {
        return TripCalculationResponse.fromJson(
          jsonDecode(response.body) as Map<String, dynamic>,
        );
      } else if (response.statusCode == 401) {
        throw UnauthorizedException('Unauthorized');
      } else {
        throw ServerException('Failed to start search: ${response.statusCode}');
      }
    } on http.ClientException catch (e) {
      throw NetworkException('Network error: $e');
    } catch (e) {
      rethrow;
    }
  }

  static Future<TripDetails> getTripDetails(int tripId) async {
    try {
      final response = await http.get(
        Uri.parse('$_baseUrl/trips/$tripId'),
        headers: TokenStorage.getAuthHeaders(),
      );

      if (response.statusCode == 200) {
        return TripDetails.fromJson(
          jsonDecode(response.body) as Map<String, dynamic>,
        );
      } else if (response.statusCode == 401) {
        throw UnauthorizedException('Unauthorized');
      } else if (response.statusCode == 404) {
        throw NotFoundException('Trip not found');
      } else {
        throw ServerException(
          'Failed to get trip details: ${response.statusCode}',
        );
      }
    } on http.ClientException catch (e) {
      throw NetworkException('Network error: $e');
    } catch (e) {
      rethrow;
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

class NotFoundException implements Exception {
  final String message;
  NotFoundException(this.message);
  @override
  String toString() => message;
}

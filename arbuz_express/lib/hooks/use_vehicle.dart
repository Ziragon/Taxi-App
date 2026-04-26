import 'dart:convert';
import 'package:http/http.dart' as http;
import '../config/api_config.dart';
import '../services/token_storage.dart';

class UseVehicle {
  Future<VehicleResult> addVehicle({
    required String brand,
    required String model,
    required int year,
    required String color,
    required String licensePlate,
    String vehicleClass = 'COMFORT',
  }) async {
    try {
      final response = await http.post(
        Uri.parse(ApiConfig.vehicles),
        headers: TokenStorage.getAuthHeaders(),
        body: jsonEncode({
          "brand": brand,
          "model": model,
          "year": year,
          "color": color,
          "licensePlate": licensePlate,
          "vehicleClass": vehicleClass,
        }),
      );

      if (response.statusCode == 200 || response.statusCode == 201) {
        return VehicleResult(success: true);
      } else {
        return VehicleResult(
          success: false,
          error: 'Ошибка добавления авто: ${response.statusCode}',
        );
      }
    } catch (e) {
      return VehicleResult(success: false, error: 'Ошибка сети: $e');
    }
  }
}

class VehicleResult {
  final bool success;
  final String? error;

  VehicleResult({required this.success, this.error});
}

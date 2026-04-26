class ApiConfig {
  static const String baseUrl = 'http://192.168.0.11:8000/api/v1';

  static const String authRegister = '$baseUrl/auth/register';
  static const String authLogin = '$baseUrl/auth/login';
  static const String profilesDriver = '$baseUrl/profiles/driver';
  static const String profilesPassenger = '$baseUrl/profiles/passenger';
  static const String vehicles = '$baseUrl/vehicles';
}

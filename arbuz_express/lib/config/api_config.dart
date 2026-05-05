class ApiConfig {
  
  static const String baseUrl = 'http://192.168.0.10:8000/api/v1';
  static const String authRegister = '$baseUrl/auth/register';
  static const String authLogin = '$baseUrl/auth/login';
  static const String authLogout = '$baseUrl/auth/logout';
  static const String profilesDriver = '$baseUrl/profiles/driver';
  static const String profilesPassenger = '$baseUrl/profiles/passenger';
  static const String vehicles = '$baseUrl/vehicles';
  static const String paymentMethods = '$baseUrl/payment-methods';
  static const String payoutAccounts = '$baseUrl/payout-accounts';
}

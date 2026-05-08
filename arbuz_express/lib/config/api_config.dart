class ApiConfig {
  static const String host = '192.168.0.10';
  static const int apiPort = 8000;
  static const int wsPort = 8083;

  static const String baseUrl = 'http://$host:$apiPort/api/v1';
  static const String wsUrl = 'ws://$host:$wsPort/ws/notifications/websocket';

  static const String authRegister = '$baseUrl/auth/register';
  static const String authLogin = '$baseUrl/auth/login';
  static const String authLogout = '$baseUrl/auth/logout';
  static const String profilesDriver = '$baseUrl/profiles/driver';
  static const String profilesPassenger = '$baseUrl/profiles/passenger';
  static const String vehicles = '$baseUrl/vehicles';
  static const String paymentMethods = '$baseUrl/payment-methods';
  static const String payoutAccounts = '$baseUrl/payout-accounts';
  static const String trips = '$baseUrl/trips';
  static const String tripsStartSearch = '$baseUrl/trips/start-search';

  static const String driverOnline = '$baseUrl/driver/online';
  static const String driverOffline = '$baseUrl/driver/offline';
}
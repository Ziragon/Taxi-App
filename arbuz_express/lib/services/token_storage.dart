class TokenStorage {
  static String? accessToken;
  static String? userRole;

  static Map<String, String> getAuthHeaders() {
    return {
      'Content-Type': 'application/json',
      'Authorization': 'Bearer $accessToken',
    };
  }
}

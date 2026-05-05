import 'dart:convert';
import 'package:flutter_stripe/flutter_stripe.dart';
import 'package:http/http.dart' as http;
import '../config/api_config.dart';
import '../services/token_storage.dart';

class UsePayment {
  static const String stripePublishableKey =
      'pk_test_51RiD22QEDYKwRuF49r5tJQ3oVIJyl5yN10Ghmk8FGXXwMpToDO0SEwfoCCcd8tSd3VfP5bDFMMBTffnfhq37SWOP00tZz2jKpP';

  String _getEndpoint() {
    return TokenStorage.userRole == 'driver'
        ? ApiConfig.payoutAccounts
        : ApiConfig.paymentMethods;
  }

  Future<String?> createStripeCredential({
    required String cardholderName,
    String? postalCode,
  }) async {
    try {
      if (TokenStorage.userRole == 'driver') {
        final tokenData = await Stripe.instance.createToken(
          const CreateTokenParams.card(params: CardTokenParams()),
        );
        return tokenData.id;
      } else {
        final paymentMethod = await Stripe.instance.createPaymentMethod(
          params: PaymentMethodParams.card(
            paymentMethodData: PaymentMethodData(
              billingDetails: BillingDetails(
                name: cardholderName,
                address: Address(
                  city: null,
                  country: null,
                  line1: null,
                  line2: null,
                  postalCode: postalCode,
                  state: null,
                ),
              ),
            ),
          ),
        );
        return paymentMethod.id;
      }
    } catch (e) {
      throw Exception('Stripe error: $e');
    }
  }

  Future<int?> addPaymentMethodToBackend(
    String stripeId,
    String lastFour,
  ) async {
    final bool isDriver = TokenStorage.userRole == 'driver';
    Map<String, dynamic> bodyMap;

    if (isDriver) {
      bodyMap = {'stripeAccountId': stripeId, 'lastFour': lastFour};
    } else {
      bodyMap = {'stripePaymentMethodId': stripeId, 'setAsDefault': false};
    }

    final response = await http.post(
      Uri.parse(_getEndpoint()),
      headers: TokenStorage.getAuthHeaders(),
      body: jsonEncode(bodyMap),
    );

    if (response.statusCode >= 200 && response.statusCode < 300) {
      final data = jsonDecode(response.body);
      return data['id'];
    } else {
      throw Exception('Server error: ${response.statusCode} ${response.body}');
    }
  }

  Future<bool> setDefaultPaymentMethod(int paymentMethodId) async {
    try {
      final response = await http.put(
        Uri.parse('${_getEndpoint()}/$paymentMethodId/set-default'),
        headers: TokenStorage.getAuthHeaders(),
      );
      return response.statusCode >= 200 && response.statusCode < 300;
    } catch (e) {
      return false;
    }
  }

  Future<List<Map<String, dynamic>>> getPaymentMethods() async {
    try {
      final response = await http.get(
        Uri.parse(_getEndpoint()),
        headers: TokenStorage.getAuthHeaders(),
      );
      if (response.statusCode >= 200 && response.statusCode < 300) {
        final List<dynamic> data = jsonDecode(response.body);
        return data.cast<Map<String, dynamic>>();
      }
      return [];
    } catch (e) {
      return [];
    }
  }

  Future<bool> deletePaymentMethod(int paymentMethodId) async {
    try {
      final response = await http.delete(
        Uri.parse('${_getEndpoint()}/$paymentMethodId'),
        headers: TokenStorage.getAuthHeaders(),
      );
      return response.statusCode >= 200 && response.statusCode < 300;
    } catch (e) {
      return false;
    }
  }
}

import 'dart:convert';
import 'package:flutter_stripe/flutter_stripe.dart';
import 'package:http/http.dart' as http;
import '../config/api_config.dart';
import '../services/token_storage.dart';

class UsePayment {
  static const String stripePublishableKey =
      'pk_test_51RiD22QEDYKwRuF49r5tJQ3oVIJyl5yN10Ghmk8FGXXwMpToDO0SEwfoCCcd8tSd3VfP5bDFMMBTffnfhq37SWOP00tZz2jKpP';

  Future<String?> createStripePaymentMethod({
    required String cardholderName,
    String? postalCode,
  }) async {
    try {
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
    } catch (e) {
      throw Exception('Stripe card error: $e');
    }
  }

  Future<int?> addPaymentMethodToBackend(String stripePaymentMethodId) async {
    try {
      final response = await http.post(
        Uri.parse(ApiConfig.paymentMethods),
        headers: TokenStorage.getAuthHeaders(),
        body: jsonEncode({
          'stripePaymentMethodId': stripePaymentMethodId,
          'setAsDefault': false,
        }),
      );

      if (response.statusCode >= 200 && response.statusCode < 300) {
        final data = jsonDecode(response.body);
        return data['id'];
      }

      return null;
    } catch (e) {
      return null;
    }
  }

  Future<bool> setDefaultPaymentMethod(int paymentMethodId) async {
    try {
      final response = await http.put(
        Uri.parse('${ApiConfig.paymentMethods}/$paymentMethodId/set-default'),
        headers: TokenStorage.getAuthHeaders(),
      );

      return response.statusCode >= 200 && response.statusCode < 300;
    } catch (e) {
      return false;
    }
  }
}

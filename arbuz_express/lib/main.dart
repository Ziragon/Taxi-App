import 'package:arbuz_express/screens/auth_screen.dart';
import 'package:flutter/material.dart';
import 'package:flutter_stripe/flutter_stripe.dart';

import 'hooks/use_payment.dart';

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();
  Stripe.publishableKey = UsePayment.stripePublishableKey;
  await Stripe.instance.applySettings();

  runApp(const ArbuzExpressApp());
}

class ArbuzExpressApp extends StatelessWidget {
  const ArbuzExpressApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      debugShowCheckedModeBanner: false,
      title: 'Arbuz Express',
      theme: ThemeData(
        useMaterial3: true,
        brightness: Brightness.dark,
        fontFamily: 'Roboto',
        scaffoldBackgroundColor: const Color(0xFF0A0A0C),
      ),
      home: const AuthScreen(),
    );
  }
}

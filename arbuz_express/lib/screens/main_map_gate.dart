import 'package:flutter/material.dart';
import 'package:arbuz_express/screens/home_map_screen.dart';
import 'package:arbuz_express/screens/driver_map_screen.dart';

class MainMapGate extends StatelessWidget {
  final String role;
  final bool showVerificationBanner;

  const MainMapGate({
    super.key,
    required this.role,
    this.showVerificationBanner = false,
  });

  @override
  Widget build(BuildContext context) {
    if (role.toLowerCase() == 'driver') {
      return DriverMapScreen(showVerificationBanner: showVerificationBanner);
    }
    return const HomeMapScreen();
  }
}

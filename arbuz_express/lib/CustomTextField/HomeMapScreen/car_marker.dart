import 'package:flutter/material.dart';

class CarMarker extends StatelessWidget {
  const CarMarker({super.key});

  @override
  Widget build(BuildContext context) {
    return Container(
      decoration: BoxDecoration(
        color: const Color(0xFF1A1A1E),
        shape: BoxShape.circle,
        border: Border.all(color: const Color(0xFFFFC107), width: 2.5),
        boxShadow: [
          BoxShadow(
            color: const Color(0xFFFFC107).withOpacity(0.4),
            blurRadius: 12,
            spreadRadius: 2,
          ),
        ],
      ),
      child: const Center(
        child: Icon(
          Icons.navigation_rounded,
          color: Color(0xFFFFC107),
          size: 22,
        ),
      ),
    );
  }
}

import 'package:flutter/material.dart';

class DestinationMarker extends StatelessWidget {
  const DestinationMarker({super.key});

  @override
  Widget build(BuildContext context) {
    return ColorFiltered(
      colorFilter: const ColorFilter.matrix([
        -1.0,
        0.0,
        0.0,
        0.0,
        255.0,
        0.0,
        -1.0,
        0.0,
        0.0,
        255.0,
        0.0,
        0.0,
        -1.0,
        0.0,
        255.0,
        0.0,
        0.0,
        0.0,
        1.0,
        0.0,
      ]),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          Container(
            padding: const EdgeInsets.all(6),
            decoration: const BoxDecoration(
              color: Color(0xFFFF5722),
              shape: BoxShape.circle,
              boxShadow: [
                BoxShadow(
                  color: Color(0x4DFF5722),
                  blurRadius: 12,
                  spreadRadius: 4,
                ),
              ],
            ),
            child: const Icon(
              Icons.flag_rounded,
              color: Colors.white,
              size: 20,
            ),
          ),
        ],
      ),
    );
  }
}

import 'package:flutter/material.dart';

class TariffInfoCard extends StatelessWidget {
  final int nearbyCars;
  final String weatherTariff;
  final String distanceTariff;

  const TariffInfoCard({
    super.key,
    required this.nearbyCars,
    required this.weatherTariff,
    required this.distanceTariff,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 10),
      decoration: BoxDecoration(
        color: const Color(0xFF151518),
        borderRadius: BorderRadius.circular(14),
        border: Border.all(color: Colors.white.withOpacity(0.04)),
      ),
      child: Column(
        children: [
          Row(
            children: [
              const Icon(
                Icons.local_taxi_rounded,
                color: Color(0xFFFFC107),
                size: 16,
              ),
              const SizedBox(width: 8),
              Text(
                '$nearbyCars машин рядом',
                style: const TextStyle(
                  color: Colors.white,
                  fontSize: 13,
                  fontWeight: FontWeight.w500,
                ),
              ),
            ],
          ),
          const SizedBox(height: 6),
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              const Text(
                'Надбавка за погоду',
                style: TextStyle(color: Colors.white54, fontSize: 12),
              ),
              Text(
                weatherTariff,
                style: const TextStyle(color: Colors.white, fontSize: 12),
              ),
            ],
          ),
          const SizedBox(height: 4),
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              const Text(
                'Тариф за расстояние',
                style: TextStyle(color: Colors.white54, fontSize: 12),
              ),
              Text(
                distanceTariff,
                style: const TextStyle(color: Colors.white, fontSize: 12),
              ),
            ],
          ),
        ],
      ),
    );
  }
}

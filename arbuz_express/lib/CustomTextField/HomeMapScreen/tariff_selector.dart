import 'package:arbuz_express/models/trip_models.dart';
import 'package:flutter/material.dart';

class TariffSelector extends StatelessWidget {
  final List<Tariff> tariffs;
  final int selectedTariff;
  final ValueChanged<int> onTariffSelected;

  const TariffSelector({
    super.key,
    required this.tariffs,
    required this.selectedTariff,
    required this.onTariffSelected,
  });

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      height: 94,
      child: ListView.separated(
        scrollDirection: Axis.horizontal,
        itemCount: tariffs.length,
        separatorBuilder: (_, __) => const SizedBox(width: 12),
        itemBuilder: (context, index) {
          final tariff = tariffs[index];
          final selected = selectedTariff == index;
          final icon = switch (tariff.tripClass.toUpperCase()) {
            'ECONOMY' => Icons.directions_car,
            'BUSINESS' => Icons.workspace_premium,
            _ => Icons.airport_shuttle,
          };

          return GestureDetector(
            onTap: () => onTariffSelected(index),
            child: Container(
              width: 104,
              decoration: BoxDecoration(
                color: selected
                    ? const Color(0x1AFFC107)
                    : const Color(0xFF151518),
                borderRadius: BorderRadius.circular(18),
                border: Border.all(
                  color: selected
                      ? const Color(0xFFFFC107)
                      : Colors.white.withOpacity(0.04),
                  width: selected ? 2 : 1,
                ),
              ),
              child: Column(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  Icon(
                    icon,
                    color: selected ? const Color(0xFFFFC107) : Colors.white54,
                    size: 26,
                  ),
                  const SizedBox(height: 6),
                  Text(
                    tariff.tripClass,
                    style: TextStyle(
                      color: selected ? Colors.white : Colors.white70,
                      fontSize: 13,
                      fontWeight: FontWeight.w500,
                    ),
                  ),
                  Text(
                    '${tariff.price} ₽',
                    style: const TextStyle(
                      color: Color(0xFFFFC107),
                      fontSize: 13,
                      fontWeight: FontWeight.w800,
                    ),
                  ),
                ],
              ),
            ),
          );
        },
      ),
    );
  }
}

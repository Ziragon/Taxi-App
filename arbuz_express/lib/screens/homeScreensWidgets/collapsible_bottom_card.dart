import 'package:arbuz_express/CustomTextField/HomeMapScreen/tariff_selector.dart';
import 'package:arbuz_express/models/trip_models.dart';
import 'package:arbuz_express/widgets/app_ui.dart';
import 'package:flutter/material.dart';

import 'address_input_row.dart';

class CollapsibleBottomCard extends StatelessWidget {
  final bool isCollapsed;
  final VoidCallback onToggle;
  final TextEditingController fromController;
  final TextEditingController toController;
  final String fromHint;
  final String toHint;
  final IconData fromIcon;
  final IconData toIcon;
  final VoidCallback onGetCurrentLocation;
  final ValueChanged<String> onFromChanged;
  final ValueChanged<String> onToChanged;
  final bool showTariffs;
  final int selectedTariff;
  final ValueChanged<int> onTariffSelected;
  final VoidCallback? onOrderPressed;
  final List<Tariff>? tariffs;

  const CollapsibleBottomCard({
    super.key,
    required this.isCollapsed,
    required this.onToggle,
    required this.fromController,
    required this.toController,
    required this.fromHint,
    required this.toHint,
    required this.fromIcon,
    required this.toIcon,
    required this.onGetCurrentLocation,
    required this.onFromChanged,
    required this.onToChanged,
    required this.showTariffs,
    required this.selectedTariff,
    required this.onTariffSelected,
    this.onOrderPressed,
    this.tariffs,
  });

  @override
  Widget build(BuildContext context) {
    final hasTariffs = tariffs != null && tariffs!.isNotEmpty;
    final buttonLabel = showTariffs ? 'Заказать' : 'Рассчитать';
    final buttonEnabled = onOrderPressed != null && (!showTariffs || hasTariffs);

    return GlassCard(
      padding: const EdgeInsets.fromLTRB(16, 8, 16, 16),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          GestureDetector(
            onTap: onToggle,
            child: Container(
              width: 40,
              height: 4,
              margin: const EdgeInsets.only(bottom: 12),
              decoration: BoxDecoration(
                color: Colors.white24,
                borderRadius: BorderRadius.circular(2),
              ),
            ),
          ),
          AnimatedSize(
            duration: const Duration(milliseconds: 300),
            child: isCollapsed
                ? const SizedBox(
                    width: double.infinity,
                    height: 20,
                    child: Center(
                      child: Text(
                        'Развернуть',
                        style: TextStyle(color: Colors.white38, fontSize: 12),
                      ),
                    ),
                  )
                : Column(
                    children: [
                      AddressInputRow(
                        controller: fromController,
                        hint: fromHint,
                        icon: fromIcon,
                        isFrom: true,
                        onLocationTap: onGetCurrentLocation,
                        onChanged: onFromChanged,
                      ),
                      const Divider(color: Colors.white10, height: 1),
                      AddressInputRow(
                        controller: toController,
                        hint: toHint,
                        icon: toIcon,
                        isFrom: false,
                        onChanged: onToChanged,
                      ),
                      if (showTariffs) ...[
                        const SizedBox(height: 12),
                        if (hasTariffs)
                          TariffSelector(
                            tariffs: tariffs!,
                            selectedTariff: selectedTariff,
                            onTariffSelected: onTariffSelected,
                          )
                        else
                          Container(
                            width: double.infinity,
                            padding: const EdgeInsets.symmetric(
                              horizontal: 16,
                              vertical: 14,
                            ),
                            decoration: BoxDecoration(
                              color: const Color(0xFF1A1A1E),
                              borderRadius: BorderRadius.circular(16),
                              border: Border.all(
                                color: const Color(0xFFFFC107).withOpacity(0.2),
                                width: 1,
                              ),
                            ),
                            child: Row(
                              children: [
                                const Icon(
                                  Icons.info_outline_rounded,
                                  color: Color(0xFFFFC107),
                                  size: 20,
                                ),
                                const SizedBox(width: 12),
                                Expanded(
                                  child: Text(
                                    'Нет доступных машин для выбранного маршрута',
                                    style: TextStyle(
                                      color: Colors.white.withOpacity(0.9),
                                      fontSize: 13,
                                      fontWeight: FontWeight.w500,
                                    ),
                                  ),
                                ),
                              ],
                            ),
                          ),
                      ],
                      const SizedBox(height: 16),
                      Row(
                        children: [
                          if (showTariffs && hasTariffs)
                            CircleIconButton(
                              icon: Icons.map_rounded,
                              onTap: () {},
                              color: const Color(0xFF1A1A1E),
                            ),
                          if (showTariffs && hasTariffs)
                            const SizedBox(width: 12),
                          Expanded(
                            child: PrimaryButton(
                              label: buttonLabel,
                              onPressed: buttonEnabled ? onOrderPressed : null,
                              enabled: buttonEnabled,
                            ),
                          ),
                        ],
                      ),
                    ],
                  ),
          ),
        ],
      ),
    );
  }
}

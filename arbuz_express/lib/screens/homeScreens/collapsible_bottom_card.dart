// collapsible_bottom_card.dart
import 'package:flutter/material.dart';
import 'package:arbuz_express/widgets/app_ui.dart';
import 'package:arbuz_express/CustomTextField/HomeMapScreen/tariff_selector.dart';
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
  final VoidCallback? onStatsPressed;

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
    this.onStatsPressed,
  });

  @override
  Widget build(BuildContext context) {
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
                        TariffSelector(
                          selectedTariff: selectedTariff,
                          onTariffSelected: onTariffSelected,
                        ),
                      ],
                      const SizedBox(height: 16),
                      Row(
                        children: [
                          if (showTariffs)
                            CircleIconButton(
                              icon: Icons.tune_rounded,
                              onTap: onStatsPressed ?? () {},
                              color: const Color(0xFF1A1A1E),
                            ),
                          if (showTariffs) const SizedBox(width: 12),
                          Expanded(
                            child: PrimaryButton(
                              label: 'Заказать',
                              onPressed: onOrderPressed,
                              enabled: onOrderPressed != null,
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

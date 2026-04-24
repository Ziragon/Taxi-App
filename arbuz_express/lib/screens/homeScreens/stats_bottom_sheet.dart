import 'package:flutter/material.dart';
import 'package:arbuz_express/widgets/app_ui.dart';

class StatsBottomSheet extends StatefulWidget {
  final int nearbyCars;
  final String weatherTariff;
  final String distanceTariff;
  final int distanceBaseRaw;
  final int weatherSurchargeRaw;
  final int selectedTariff;
  final double totalTariff;

  const StatsBottomSheet({
    super.key,
    required this.nearbyCars,
    required this.weatherTariff,
    required this.distanceTariff,
    required this.distanceBaseRaw,
    required this.weatherSurchargeRaw,
    required this.selectedTariff,
    required this.totalTariff,
  });

  @override
  State<StatsBottomSheet> createState() => _StatsBottomSheetState();
}

class _StatsBottomSheetState extends State<StatsBottomSheet> {
  String _paymentMethod = 'Наличные';

  @override
  Widget build(BuildContext context) {
    final driverNames = [
      'Рамшан',
      'Аслан',
      'Алина',
      'Самир',
      'Анатолий',
      'Ашот',
      'Хамиль',
      'Снежанна',
    ];
    final randomDriver = driverNames[widget.nearbyCars % driverNames.length];
    final tariffSurcharge = widget.selectedTariff == 0
        ? 0
        : widget.selectedTariff == 1
        ? 170
        : 550;
    final totalWithTariff = (widget.totalTariff + tariffSurcharge).round();

    return Container(
      decoration: const BoxDecoration(color: Colors.transparent),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          const SizedBox(height: 20),
          GlassCard(
            padding: const EdgeInsets.all(20),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    const Text(
                      'СТАТИСТИКА ПОЕЗДКИ',
                      style: TextStyle(
                        color: Colors.white70,
                        fontSize: 12,
                        fontWeight: FontWeight.w900,
                        letterSpacing: 1.2,
                      ),
                    ),
                    CircleIconButton(
                      icon: Icons.close_rounded,
                      onTap: () => Navigator.pop(context),
                      color: const Color(0xFF1A1A1E),
                      size: 36,
                    ),
                  ],
                ),
                const SizedBox(height: 20),
                Row(
                  children: [
                    const Icon(
                      Icons.local_taxi_rounded,
                      color: Color(0xFFFFC107),
                      size: 20,
                    ),
                    const SizedBox(width: 10),
                    Text(
                      '${widget.nearbyCars} машин рядом',
                      style: const TextStyle(
                        color: Colors.white,
                        fontSize: 14,
                        fontWeight: FontWeight.w500,
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 12),
                Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    const Text(
                      'Надбавка за погоду',
                      style: TextStyle(color: Colors.white54, fontSize: 13),
                    ),
                    Text(
                      widget.weatherTariff,
                      style: const TextStyle(
                        color: Colors.white,
                        fontSize: 13,
                        fontWeight: FontWeight.w500,
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 8),
                Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    const Text(
                      'Тариф за расстояние',
                      style: TextStyle(color: Colors.white54, fontSize: 13),
                    ),
                    Text(
                      widget.distanceTariff,
                      style: const TextStyle(
                        color: Colors.white,
                        fontSize: 13,
                        fontWeight: FontWeight.w500,
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 8),
                Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    const Text(
                      'Минимальная стоимость',
                      style: TextStyle(color: Colors.white54, fontSize: 13),
                    ),
                    const Text(
                      '50 ₽',
                      style: TextStyle(
                        color: Colors.white,
                        fontSize: 13,
                        fontWeight: FontWeight.w500,
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 8),
                Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    const Text(
                      'Выбранный тариф',
                      style: TextStyle(color: Colors.white54, fontSize: 13),
                    ),
                    Text(
                      widget.selectedTariff == 0
                          ? 'Эконом'
                          : widget.selectedTariff == 1
                          ? 'Комфорт'
                          : 'Бизнес',
                      style: const TextStyle(
                        color: Color(0xFFFFC107),
                        fontSize: 13,
                        fontWeight: FontWeight.w600,
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 8),
                Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    const Text(
                      'Надбавка за тариф',
                      style: TextStyle(color: Colors.white54, fontSize: 13),
                    ),
                    Text(
                      tariffSurcharge == 0 ? '0 ₽' : '+$tariffSurcharge ₽',
                      style: const TextStyle(
                        color: Colors.white,
                        fontSize: 13,
                        fontWeight: FontWeight.w500,
                      ),
                    ),
                  ],
                ),
                const Divider(color: Colors.white10, height: 24),
                Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    Row(
                      children: [
                        const Icon(
                          Icons.emoji_emotions_rounded,
                          color: Color(0xFFFFC107),
                          size: 18,
                        ),
                        const SizedBox(width: 8),
                        Text(
                          'Водитель трезвый',
                          style: TextStyle(color: Colors.white70, fontSize: 12),
                        ),
                      ],
                    ),
                    Container(
                      padding: const EdgeInsets.symmetric(
                        horizontal: 8,
                        vertical: 2,
                      ),
                      decoration: BoxDecoration(
                        color: Colors.green.withOpacity(0.2),
                        borderRadius: BorderRadius.circular(12),
                      ),
                      child: const Text(
                        'К СОЖАЛЕНИЮ',
                        style: TextStyle(
                          color: Colors.green,
                          fontSize: 10,
                          fontWeight: FontWeight.w800,
                        ),
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 8),
                Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    Row(
                      children: [
                        const Icon(
                          Icons.person_rounded,
                          color: Color(0xFFFFC107),
                          size: 18,
                        ),
                        const SizedBox(width: 8),
                        Text(
                          'Его зовут',
                          style: TextStyle(color: Colors.white70, fontSize: 12),
                        ),
                      ],
                    ),
                    Text(
                      randomDriver,
                      style: const TextStyle(
                        color: Color(0xFFFFC107),
                        fontSize: 12,
                        fontWeight: FontWeight.w600,
                      ),
                    ),
                  ],
                ),
                const Divider(color: Colors.white10, height: 24),
                Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    const Text(
                      'ИТОГО К ОПЛАТЕ',
                      style: TextStyle(
                        color: Colors.white,
                        fontSize: 16,
                        fontWeight: FontWeight.w900,
                        letterSpacing: 1,
                      ),
                    ),
                    Text(
                      '$totalWithTariff ₽',
                      style: const TextStyle(
                        color: Color(0xFFFFC107),
                        fontSize: 22,
                        fontWeight: FontWeight.w900,
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 24),
                Container(
                  padding: const EdgeInsets.all(12),
                  decoration: BoxDecoration(
                    color: Colors.white.withOpacity(0.03),
                    borderRadius: BorderRadius.circular(16),
                    border: Border.all(color: Colors.white.withOpacity(0.06)),
                  ),
                  child: Column(
                    children: [
                      const Center(
                        child: Text(
                          'Способ оплаты',
                          style: TextStyle(color: Colors.white54, fontSize: 14),
                        ),
                      ),
                      const SizedBox(height: 12),
                      Row(
                        mainAxisAlignment: MainAxisAlignment.center,
                        children: [
                          GestureDetector(
                            onTap: () {
                              setState(() {
                                _paymentMethod = 'Наличные';
                              });
                            },
                            child: Container(
                              padding: const EdgeInsets.symmetric(
                                horizontal: 20,
                                vertical: 10,
                              ),
                              decoration: BoxDecoration(
                                color: _paymentMethod == 'Наличные'
                                    ? const Color(0xFFFFC107).withOpacity(0.15)
                                    : Colors.transparent,
                                borderRadius: BorderRadius.circular(24),
                                border: Border.all(
                                  color: _paymentMethod == 'Наличные'
                                      ? const Color(0xFFFFC107)
                                      : Colors.white.withOpacity(0.1),
                                  width: 1.5,
                                ),
                              ),
                              child: Row(
                                mainAxisSize: MainAxisSize.min,
                                children: [
                                  Icon(
                                    Icons.money_rounded,
                                    color: _paymentMethod == 'Наличные'
                                        ? const Color(0xFFFFC107)
                                        : Colors.white54,
                                    size: 18,
                                  ),
                                  const SizedBox(width: 8),
                                  Text(
                                    'Наличные',
                                    style: TextStyle(
                                      color: _paymentMethod == 'Наличные'
                                          ? Colors.white
                                          : Colors.white70,
                                      fontSize: 14,
                                      fontWeight: FontWeight.w500,
                                    ),
                                  ),
                                ],
                              ),
                            ),
                          ),
                          const SizedBox(width: 16),
                          GestureDetector(
                            onTap: () {
                              setState(() {
                                _paymentMethod = 'Безнал';
                              });
                            },
                            child: Container(
                              padding: const EdgeInsets.symmetric(
                                horizontal: 20,
                                vertical: 10,
                              ),
                              decoration: BoxDecoration(
                                color: _paymentMethod == 'Безнал'
                                    ? const Color(0xFFFFC107).withOpacity(0.15)
                                    : Colors.transparent,
                                borderRadius: BorderRadius.circular(24),
                                border: Border.all(
                                  color: _paymentMethod == 'Безнал'
                                      ? const Color(0xFFFFC107)
                                      : Colors.white.withOpacity(0.1),
                                  width: 1.5,
                                ),
                              ),
                              child: Row(
                                mainAxisSize: MainAxisSize.min,
                                children: [
                                  Icon(
                                    Icons.credit_card_rounded,
                                    color: _paymentMethod == 'Безнал'
                                        ? const Color(0xFFFFC107)
                                        : Colors.white54,
                                    size: 18,
                                  ),
                                  const SizedBox(width: 8),
                                  Text(
                                    'Безнал',
                                    style: TextStyle(
                                      color: _paymentMethod == 'Безнал'
                                          ? Colors.white
                                          : Colors.white70,
                                      fontSize: 14,
                                      fontWeight: FontWeight.w500,
                                    ),
                                  ),
                                ],
                              ),
                            ),
                          ),
                        ],
                      ),
                    ],
                  ),
                ),
                const SizedBox(height: 20),
                SizedBox(
                  width: double.infinity,
                  child: PrimaryButton(
                    label: 'ПОНЯТНО',
                    onPressed: () => Navigator.pop(context),
                  ),
                ),
              ],
            ),
          ),
          const SizedBox(height: 20),
        ],
      ),
    );
  }
}

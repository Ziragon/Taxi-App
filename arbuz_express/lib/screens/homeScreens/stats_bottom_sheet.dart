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
  final Function(Map<String, String>) onAccept;

  const StatsBottomSheet({
    super.key,
    required this.nearbyCars,
    required this.weatherTariff,
    required this.distanceTariff,
    required this.distanceBaseRaw,
    required this.weatherSurchargeRaw,
    required this.selectedTariff,
    required this.totalTariff,
    required this.onAccept,
  });

  @override
  State<StatsBottomSheet> createState() => _StatsBottomSheetState();
}

class _StatsBottomSheetState extends State<StatsBottomSheet> {
  String _paymentMethod = 'Наличные';
  String _hookahOption = 'Не надо';
  String _driverOption = 'По умолчанию';
  String _musicOption = 'Реп';

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
    final displayDriver = _driverOption == 'Никита' ? 'Никита' : randomDriver;
    final tariffSurcharge = widget.selectedTariff == 0
        ? 0
        : widget.selectedTariff == 1
        ? 170
        : 550;
    final totalWithTariff = (widget.totalTariff + tariffSurcharge).round();

    final carModels = [
      'Hyundai Solaris',
      'Kia Rio',
      'Skoda Rapid',
      'Lada Vesta',
      'Toyota Camry',
    ];
    final carNumbers = ['А123ВС', 'М777УН', 'О999ОО', 'Т543ХТ', 'Е321КЕ'];
    final finalCarModel = carModels[widget.nearbyCars % carModels.length];
    final finalCarNumber = carNumbers[widget.nearbyCars % carNumbers.length];

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
                      displayDriver,
                      style: const TextStyle(
                        color: Color(0xFFFFC107),
                        fontSize: 12,
                        fontWeight: FontWeight.w600,
                      ),
                    ),
                  ],
                ),
                const Divider(color: Colors.white10, height: 24),
                const SizedBox(height: 8),
                Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    const Text(
                      'Разогреть кальян?',
                      style: TextStyle(color: Colors.white70, fontSize: 13),
                    ),
                    Row(
                      children: [
                        _buildOptionChip('Да', _hookahOption, (value) {
                          setState(() => _hookahOption = value);
                        }),
                        const SizedBox(width: 8),
                        _buildOptionChip('Не надо', _hookahOption, (value) {
                          setState(() => _hookahOption = value);
                        }),
                      ],
                    ),
                  ],
                ),
                const SizedBox(height: 12),
                Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    const Text(
                      'Водитель',
                      style: TextStyle(color: Colors.white70, fontSize: 13),
                    ),
                    Row(
                      children: [
                        _buildOptionChip('По умолчанию', _driverOption, (
                          value,
                        ) {
                          setState(() => _driverOption = value);
                        }),
                        const SizedBox(width: 8),
                        _buildOptionChip('Никита', _driverOption, (value) {
                          setState(() => _driverOption = value);
                        }),
                      ],
                    ),
                  ],
                ),
                const SizedBox(height: 12),
                Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    const Text(
                      'Музыка',
                      style: TextStyle(color: Colors.white70, fontSize: 13),
                    ),
                    Row(
                      children: [
                        _buildOptionChip('Реп', _musicOption, (value) {
                          setState(() => _musicOption = value);
                        }),
                        const SizedBox(width: 8),
                        _buildOptionChip('Поп', _musicOption, (value) {
                          setState(() => _musicOption = value);
                        }),
                        const SizedBox(width: 8),
                        _buildOptionChip('Глухой водитель', _musicOption, (
                          value,
                        ) {
                          setState(() => _musicOption = value);
                        }),
                      ],
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
                    label: 'ПРИНЯТЬ',
                    onPressed: () {
                      widget.onAccept({
                        'paymentMethod': _paymentMethod,
                        'hookah': _hookahOption,
                        'driverOption': _driverOption,
                        'music': _musicOption,
                        'driverName': displayDriver,
                        'carModel': finalCarModel,
                        'carNumber': finalCarNumber,
                        'avatarUrl':
                            'https://avatars.mds.yandex.net/i?id=fd7b56ab40f07b18eb7defbfb2d2eaa2_l-5075316-images-thumbs&n=13',
                      });
                      Navigator.pop(context);
                    },
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

  Widget _buildOptionChip(
    String label,
    String selected,
    Function(String) onSelected,
  ) {
    final isSelected = selected == label;
    return GestureDetector(
      onTap: () => onSelected(label),
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
        decoration: BoxDecoration(
          color: isSelected
              ? const Color(0xFFFFC107).withOpacity(0.2)
              : Colors.transparent,
          borderRadius: BorderRadius.circular(20),
          border: Border.all(
            color: isSelected
                ? const Color(0xFFFFC107)
                : Colors.white.withOpacity(0.2),
            width: 1,
          ),
        ),
        child: Text(
          label,
          style: TextStyle(
            color: isSelected ? const Color(0xFFFFC107) : Colors.white70,
            fontSize: 12,
            fontWeight: isSelected ? FontWeight.w600 : FontWeight.normal,
          ),
        ),
      ),
    );
  }
}

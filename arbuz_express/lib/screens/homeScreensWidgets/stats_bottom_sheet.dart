import 'package:flutter/material.dart';
import 'package:arbuz_express/widgets/app_ui.dart';
import 'package:arbuz_express/models/trip_models.dart';
import 'package:arbuz_express/services/trip_service.dart';

class StatsBottomSheet extends StatefulWidget {
  final TripCalculationResponse tripData;
  final int selectedTariff;
  final Function(Map<String, String>) onAccept;

  const StatsBottomSheet({
    super.key,
    required this.tripData,
    required this.selectedTariff,
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
  bool _isLoading = false;
  late int _activeTariffIndex;

  @override
  void initState() {
    super.initState();
    _activeTariffIndex = _resolveTariffIndex();
  }

  int _resolveTariffIndex() {
    if (widget.tripData.tariffs.isEmpty) return -1;
    if (widget.selectedTariff >= 0 &&
        widget.selectedTariff < widget.tripData.tariffs.length) {
      return widget.selectedTariff;
    }
    return 0;
  }

  Tariff? get _currentTariff {
    if (_activeTariffIndex < 0 || widget.tripData.tariffs.isEmpty) return null;
    return widget.tripData.tariffs[_activeTariffIndex];
  }

  @override
  Widget build(BuildContext context) {
    if (widget.tripData.tariffs.isEmpty) {
      return _buildErrorState('Тарифы недоступны');
    }

    final tariff = _currentTariff!;

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
    final randomDriver = driverNames[tariff.carsNearby % driverNames.length];
    final displayDriver = _driverOption == 'Никита' ? 'Никита' : randomDriver;

    final carModels = [
      'Hyundai Solaris',
      'Kia Rio',
      'Skoda Rapid',
      'Lada Vesta',
      'Toyota Camry',
    ];
    final carNumbers = ['А123ВС', 'М777УН', 'О999ОО', 'Т543ХТ', 'Е321КЕ'];
    final finalCarModel = carModels[tariff.carsNearby % carModels.length];
    final finalCarNumber = carNumbers[tariff.carsNearby % carNumbers.length];

    return Container(
      decoration: const BoxDecoration(color: Colors.transparent),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          const SizedBox(height: 20),
          GlassCard(
            padding: const EdgeInsets.all(20),
            child: SingleChildScrollView(
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
                  if (widget.tripData.tariffs.length > 1)
                    _buildTariffSwitcher(),
                  const SizedBox(height: 12),
                  Row(
                    children: [
                      const Icon(
                        Icons.local_taxi_rounded,
                        color: Color(0xFFFFC107),
                        size: 20,
                      ),
                      const SizedBox(width: 10),
                      Text(
                        '${tariff.carsNearby} машин рядом',
                        style: const TextStyle(
                          color: Colors.white,
                          fontSize: 14,
                          fontWeight: FontWeight.w500,
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 12),
                  _buildInfoRow(
                    'Расстояние',
                    '${widget.tripData.distanceKm.toStringAsFixed(1)} км',
                  ),
                  const SizedBox(height: 8),
                  _buildInfoRow(
                    'Время в пути',
                    '${widget.tripData.durationMin} мин',
                  ),
                  const SizedBox(height: 8),
                  _buildInfoRow('Базовая ставка', '${tariff.baseFare} ₽'),
                  const SizedBox(height: 8),
                  _buildInfoRow('Цена за км', '${tariff.pricePerKm} ₽'),
                  const SizedBox(height: 8),
                  _buildInfoRow(
                    'Стоимость расстояния',
                    '${tariff.distanceCost} ₽',
                  ),
                  const SizedBox(height: 8),
                  _buildInfoRow('Цена за мин', '${tariff.pricePerMin} ₽'),
                  const SizedBox(height: 8),
                  _buildInfoRow(
                    'Стоимость времени',
                    '${tariff.durationCost} ₽',
                  ),
                  const SizedBox(height: 8),
                  _buildInfoRow(
                    'Коэффициент погоды',
                    '${widget.tripData.weatherCoef.toStringAsFixed(2)}x',
                  ),
                  const SizedBox(height: 8),
                  _buildInfoRow(
                    'Коэффициент спроса',
                    '${widget.tripData.surgeCoef.toStringAsFixed(2)}x',
                  ),
                  const SizedBox(height: 8),
                  _buildInfoRow(
                    'Выбранный тариф',
                    tariff.tripClass,
                    isHighlight: true,
                  ),
                  const Divider(color: Colors.white10, height: 24),
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
                          const Text(
                            'Водитель',
                            style: TextStyle(
                              color: Colors.white70,
                              fontSize: 12,
                            ),
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
                  _buildChipRow(
                    label: 'Разогреть кальян?',
                    options: const ['Да', 'Не надо'],
                    selected: _hookahOption,
                    onSelected: (v) => setState(() => _hookahOption = v),
                  ),
                  const SizedBox(height: 12),
                  _buildChipRow(
                    label: 'Водитель',
                    options: const ['По умолчанию', 'Никита'],
                    selected: _driverOption,
                    onSelected: (v) => setState(() => _driverOption = v),
                  ),
                  const SizedBox(height: 12),
                  _buildChipRow(
                    label: 'Музыка',
                    options: const ['Реп', 'Поп'],
                    selected: _musicOption,
                    onSelected: (v) => setState(() => _musicOption = v),
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
                        '${tariff.price} ₽',
                        style: const TextStyle(
                          color: Color(0xFFFFC107),
                          fontSize: 22,
                          fontWeight: FontWeight.w900,
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 24),
                  _buildPaymentSelector(),
                  const SizedBox(height: 20),
                  SizedBox(
                    width: double.infinity,
                    child: PrimaryButton(
                      label: _isLoading ? 'ЗАГРУЗКА...' : 'ПРИНЯТЬ',
                      onPressed: _isLoading
                          ? null
                          : () => _handleAcceptOrder(
                              displayDriver: displayDriver,
                              finalCarModel: finalCarModel,
                              finalCarNumber: finalCarNumber,
                              tariff: tariff,
                            ),
                      enabled: !_isLoading,
                    ),
                  ),
                ],
              ),
            ),
          ),
          const SizedBox(height: 20),
        ],
      ),
    );
  }

  Widget _buildErrorState(String message) {
    return Container(
      decoration: const BoxDecoration(color: Colors.transparent),
      height: 200,
      child: Center(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            const Icon(Icons.error_outline, color: Color(0xFFFF5722), size: 40),
            const SizedBox(height: 12),
            Text(
              message,
              style: const TextStyle(color: Colors.white, fontSize: 16),
            ),
            const SizedBox(height: 16),
            TextButton(
              onPressed: () => Navigator.pop(context),
              child: const Text(
                'Закрыть',
                style: TextStyle(color: Color(0xFFFFC107)),
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildTariffSwitcher() {
    return SizedBox(
      height: 38,
      child: ListView.separated(
        scrollDirection: Axis.horizontal,
        itemCount: widget.tripData.tariffs.length,
        separatorBuilder: (_, __) => const SizedBox(width: 8),
        itemBuilder: (context, index) {
          final t = widget.tripData.tariffs[index];
          final isSelected = _activeTariffIndex == index;
          return GestureDetector(
            onTap: () => setState(() => _activeTariffIndex = index),
            child: Container(
              padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 8),
              decoration: BoxDecoration(
                color: isSelected
                    ? const Color(0xFFFFC107).withOpacity(0.15)
                    : Colors.transparent,
                borderRadius: BorderRadius.circular(20),
                border: Border.all(
                  color: isSelected
                      ? const Color(0xFFFFC107)
                      : Colors.white.withOpacity(0.2),
                  width: 1.5,
                ),
              ),
              child: Text(
                t.tripClass,
                style: TextStyle(
                  color: isSelected ? const Color(0xFFFFC107) : Colors.white70,
                  fontSize: 13,
                  fontWeight: isSelected ? FontWeight.w700 : FontWeight.normal,
                ),
              ),
            ),
          );
        },
      ),
    );
  }

  Widget _buildChipRow({
    required String label,
    required List<String> options,
    required String selected,
    required Function(String) onSelected,
  }) {
    return Row(
      mainAxisAlignment: MainAxisAlignment.spaceBetween,
      children: [
        Flexible(
          child: Text(
            label,
            style: const TextStyle(color: Colors.white70, fontSize: 13),
          ),
        ),
        const SizedBox(width: 8),
        Row(
          mainAxisSize: MainAxisSize.min,
          children: options
              .map(
                (option) => Padding(
                  padding: const EdgeInsets.only(left: 6),
                  child: _buildOptionChip(option, selected, onSelected),
                ),
              )
              .toList(),
        ),
      ],
    );
  }

  Widget _buildPaymentSelector() {
    return Container(
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
              _buildPaymentMethod('Наличные', Icons.money_rounded),
              const SizedBox(width: 16),
              _buildPaymentMethod('Безнал', Icons.credit_card_rounded),
            ],
          ),
        ],
      ),
    );
  }

  Future<void> _handleAcceptOrder({
    required String displayDriver,
    required String finalCarModel,
    required String finalCarNumber,
    required Tariff tariff,
  }) async {
    setState(() => _isLoading = true);

    try {
      await TripService.startSearching(
        tripId: widget.tripData.id,
        vehicleClass: tariff.tripClass,
      );

      widget.onAccept({
        'paymentMethod': _paymentMethod,
        'hookah': _hookahOption,
        'driverOption': _driverOption,
        'music': _musicOption,
        'driverName': displayDriver,
        'carModel': finalCarModel,
        'carNumber': finalCarNumber,
        'tripId': widget.tripData.id.toString(),
        'price': tariff.price.toString(),
        'distance': widget.tripData.distanceKm.toStringAsFixed(1),
        'duration': widget.tripData.durationMin.toString(),
        'avatarUrl':
            'https://avatars.mds.yandex.net/i?id=fd7b56ab40f07b18eb7defbfb2d2eaa2_l-5075316-images-thumbs&n=13',
      });

      if (mounted) Navigator.pop(context);
    } on ServerException catch (e) {
      _showErrorSnackBar('Ошибка сервера: $e');
    } on UnauthorizedException catch (e) {
      _showErrorSnackBar('Ошибка авторизации: $e');
    } on NetworkException catch (e) {
      _showErrorSnackBar('Ошибка сети: $e');
    } catch (e) {
      _showErrorSnackBar('Неизвестная ошибка: $e');
    } finally {
      if (mounted) setState(() => _isLoading = false);
    }
  }

  void _showErrorSnackBar(String message) {
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text(message),
        backgroundColor: const Color(0xFFFF5722),
        duration: const Duration(seconds: 3),
      ),
    );
  }

  Widget _buildInfoRow(
    String label,
    String value, {
    bool isHighlight = false,
  }) => Row(
    mainAxisAlignment: MainAxisAlignment.spaceBetween,
    children: [
      Text(label, style: const TextStyle(color: Colors.white54, fontSize: 13)),
      Text(
        value,
        style: TextStyle(
          color: isHighlight ? const Color(0xFFFFC107) : Colors.white,
          fontSize: 13,
          fontWeight: isHighlight ? FontWeight.w600 : FontWeight.w500,
        ),
      ),
    ],
  );

  Widget _buildPaymentMethod(String label, IconData icon) => GestureDetector(
    onTap: () => setState(() => _paymentMethod = label),
    child: Container(
      padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 10),
      decoration: BoxDecoration(
        color: _paymentMethod == label
            ? const Color(0xFFFFC107).withOpacity(0.15)
            : Colors.transparent,
        borderRadius: BorderRadius.circular(24),
        border: Border.all(
          color: _paymentMethod == label
              ? const Color(0xFFFFC107)
              : Colors.white.withOpacity(0.1),
          width: 1.5,
        ),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          Icon(
            icon,
            color: _paymentMethod == label
                ? const Color(0xFFFFC107)
                : Colors.white54,
            size: 18,
          ),
          const SizedBox(width: 8),
          Text(
            label,
            style: TextStyle(
              color: _paymentMethod == label ? Colors.white : Colors.white70,
              fontSize: 14,
              fontWeight: FontWeight.w500,
            ),
          ),
        ],
      ),
    ),
  );

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

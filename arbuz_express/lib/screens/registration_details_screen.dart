import 'package:arbuz_express/screens/home_map_screen.dart';
import 'package:arbuz_express/widgets/app_ui.dart';
import 'package:flutter/material.dart';

class RegistrationDetailsScreen extends StatefulWidget {
  const RegistrationDetailsScreen({super.key, required this.isDriver});

  final bool isDriver;

  @override
  State<RegistrationDetailsScreen> createState() =>
      _RegistrationDetailsScreenState();
}

class _RegistrationDetailsScreenState extends State<RegistrationDetailsScreen> {
  final _fioController = TextEditingController();
  final _cardController = TextEditingController();
  final _carController = TextEditingController();

  bool _isFormValid = false;

  void _validateForm() {
    final fio = _fioController.text.trim();
    final card = _cardController.text.trim().replaceAll(RegExp(r'[^0-9]'), '');
    final car = _carController.text.trim();

    setState(() {
      _isFormValid =
          fio.length > 5 &&
          card.length >= 16 &&
          (widget.isDriver ? car.isNotEmpty : true);
    });
  }

  @override
  void initState() {
    super.initState();
    _fioController.addListener(_validateForm);
    _cardController.addListener(_validateForm);
    _carController.addListener(_validateForm);
  }

  @override
  void dispose() {
    _fioController.dispose();
    _cardController.dispose();
    _carController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: const Color(0xFF0A0A0C),
      appBar: AppBar(
        backgroundColor: Colors.transparent,
        elevation: 0,
        leading: IconButton(
          icon: const Icon(
            Icons.arrow_back_ios_new_rounded,
            color: Colors.white,
          ),
          onPressed: () => Navigator.pop(context),
        ),
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(24),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const StepBadge(step: 2, total: 2),
            const SizedBox(height: 16),
            Text(
              widget.isDriver ? 'Станьте водителем' : 'Завершение регистрации',
              style: const TextStyle(
                fontSize: 30,
                fontWeight: FontWeight.w900,
                color: Colors.white,
              ),
            ),
            const SizedBox(height: 8),
            Text(
              'Заполните данные для начала работы',
              style: TextStyle(
                fontSize: 16,
                color: Colors.white.withOpacity(0.55),
              ),
            ),
            const SizedBox(height: 40),
            GlassCard(
              child: Column(
                children: [
                  CustomTextField(
                    hintText: 'Фамилия Имя Отчество',
                    icon: Icons.badge_rounded,
                    controller: _fioController,
                  ),
                  const SizedBox(height: 16),
                  CustomTextField(
                    hintText: 'Номер карты (16 цифр)',
                    icon: Icons.credit_card_rounded,
                    keyboardType: TextInputType.number,
                    controller: _cardController,
                  ),
                  if (widget.isDriver) ...[
                    const SizedBox(height: 16),
                    CustomTextField(
                      hintText: 'Номер автомобиля (например A123BC 777)',
                      icon: Icons.directions_car_filled_rounded,
                      controller: _carController,
                    ),
                  ],
                  const SizedBox(height: 36),
                  PrimaryButton(
                    label: 'Завершить',
                    onPressed: _isFormValid
                        ? () => Navigator.of(context).pushAndRemoveUntil(
                            MaterialPageRoute(
                              builder: (_) => const HomeMapScreen(),
                            ),
                            (route) => false,
                          )
                        : null,
                    enabled: _isFormValid,
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}

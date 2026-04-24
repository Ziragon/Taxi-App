import 'package:arbuz_express/screens/homeScreens/home_map_screen.dart';
import 'package:arbuz_express/widgets/app_ui.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

class CardNumberFormatter extends TextInputFormatter {
  @override
  TextEditingValue formatEditUpdate(
    TextEditingValue oldValue,
    TextEditingValue newValue,
  ) {
    String text = newValue.text.replaceAll(' ', '');
    String formatted = "";
    for (int i = 0; i < text.length; i++) {
      formatted += text[i];
      if ((i + 1) % 4 == 0 && (i + 1) != text.length) {
        formatted += " ";
      }
    }
    return TextEditingValue(
      text: formatted,
      selection: TextSelection.collapsed(offset: formatted.length),
    );
  }
}

class ExpiryDateFormatter extends TextInputFormatter {
  @override
  TextEditingValue formatEditUpdate(
    TextEditingValue oldValue,
    TextEditingValue newValue,
  ) {
    String text = newValue.text.replaceAll('/', '');
    if (text.length > 4) text = text.substring(0, 4);
    String formatted = text;
    if (text.length >= 2) {
      formatted =
          text.substring(0, 2) +
          (text.length > 2 ? '/' + text.substring(2) : '');
    }
    return TextEditingValue(
      text: formatted,
      selection: TextSelection.collapsed(offset: formatted.length),
    );
  }
}

class RegistrationDetailsScreen extends StatefulWidget {
  const RegistrationDetailsScreen({super.key, required this.isDriver});

  final bool isDriver;

  @override
  State<RegistrationDetailsScreen> createState() =>
      _RegistrationDetailsScreenState();
}

class _RegistrationDetailsScreenState extends State<RegistrationDetailsScreen> {
  final _phoneController = TextEditingController(text: '+7');
  final _fioController = TextEditingController();
  final _cardController = TextEditingController();
  final _cvvController = TextEditingController();
  final _expiryController = TextEditingController();
  final _passwordController = TextEditingController();
  final _carBrandController = TextEditingController();
  final _carModelController = TextEditingController();
  final _carPlateController = TextEditingController();
  final _carYearController = TextEditingController();
  final _carColorController = TextEditingController();

  bool _isFormValid = false;

  void _handlePrefixProtection() {
    if (!_phoneController.text.startsWith('+7')) {
      _phoneController.value = const TextEditingValue(
        text: '+7',
        selection: TextSelection.collapsed(offset: 2),
      );
    }
  }

  void _validateForm() {
    final phoneClean = _phoneController.text.replaceAll(RegExp(r'[^0-9]'), '');
    final fio = _fioController.text.trim();
    final card = _cardController.text.trim().replaceAll(' ', '');
    final cvv = _cvvController.text.trim();
    final expiry = _expiryController.text.trim();
    final password = _passwordController.text.trim();

    bool isPhoneValid = phoneClean.length == 11;

    final fioWords = fio.split(' ').where((w) => w.isNotEmpty).toList();
    bool isFioValid =
        fioWords.length == 3 && fioWords.every((w) => w.length > 3);

    bool isCardValid = card.length == 16;
    bool isCvvValid = cvv.length == 3 && int.tryParse(cvv) != null;
    bool isExpiryValid = false;
    if (expiry.length == 5 && expiry.contains('/')) {
      final parts = expiry.split('/');
      if (parts.length == 2) {
        final month = int.tryParse(parts[0]);
        final year = int.tryParse(parts[1]);
        if (month != null && year != null && month >= 1 && month <= 12) {
          final now = DateTime.now();
          final currentYear = now.year % 100;
          final currentMonth = now.month;
          if (year > currentYear ||
              (year == currentYear && month >= currentMonth)) {
            isExpiryValid = true;
          }
        }
      }
    }

    bool isPasswordValid =
        password.length >= 6 &&
        RegExp(r'[A-Z]').hasMatch(password) &&
        RegExp(r'\d').hasMatch(password) &&
        RegExp(r'[!@#$%^&*(),.?":{}|<>]').hasMatch(password);

    bool isCommonValid =
        isPhoneValid &&
        isFioValid &&
        isCardValid &&
        isCvvValid &&
        isExpiryValid &&
        isPasswordValid;

    if (widget.isDriver) {
      final brand = _carBrandController.text.trim();
      final model = _carModelController.text.trim();
      final plate = _carPlateController.text.trim().toUpperCase();
      final yearText = _carYearController.text.trim();
      final color = _carColorController.text.trim();

      final plateRegex = RegExp(
        r'^[АВЕКМНОРСТУХ]\d{3}[АВЕКМНОРСТУХ]{2}\d{2,3}$',
      );

      bool isYearValid = false;
      if (yearText.isNotEmpty) {
        final year = int.tryParse(yearText);
        final currentYear = DateTime.now().year;
        isYearValid = year != null && year >= 1900 && year <= currentYear;
      }

      bool isColorValid = color.length >= 3;

      setState(() {
        _isFormValid =
            isCommonValid &&
            brand.length >= 2 &&
            model.isNotEmpty &&
            plateRegex.hasMatch(plate) &&
            isYearValid &&
            isColorValid;
      });
    } else {
      setState(() {
        _isFormValid = isCommonValid;
      });
    }
  }

  @override
  void initState() {
    super.initState();
    _phoneController.addListener(_handlePrefixProtection);
    _phoneController.addListener(_validateForm);
    _fioController.addListener(_validateForm);
    _cardController.addListener(_validateForm);
    _cvvController.addListener(_validateForm);
    _expiryController.addListener(_validateForm);
    _passwordController.addListener(_validateForm);
    _carBrandController.addListener(_validateForm);
    _carModelController.addListener(_validateForm);
    _carPlateController.addListener(_validateForm);
    _carYearController.addListener(_validateForm);
    _carColorController.addListener(_validateForm);
  }

  @override
  void dispose() {
    _phoneController.removeListener(_handlePrefixProtection);
    _phoneController.dispose();
    _fioController.dispose();
    _cardController.dispose();
    _cvvController.dispose();
    _expiryController.dispose();
    _passwordController.dispose();
    _carBrandController.dispose();
    _carModelController.dispose();
    _carPlateController.dispose();
    _carYearController.dispose();
    _carColorController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: const Color(0xFF0A0A0C),
      body: Stack(
        children: [
          const Positioned(
            top: -100,
            left: -50,
            child: GlowOrb(size: 300, color: Color(0x15FFC107)),
          ),
          SafeArea(
            child: ListView(
              padding: const EdgeInsets.symmetric(horizontal: 24),
              children: [
                const SizedBox(height: 16),
                Row(
                  children: [
                    CircleIconButton(
                      icon: Icons.arrow_back_ios_new_rounded,
                      onTap: () => Navigator.pop(context),
                    ),
                    const SizedBox(width: 20),
                    const StepBadge(step: 2, total: 2),
                  ],
                ),
                const SizedBox(height: 32),
                const Text(
                  'Личные данные',
                  style: TextStyle(
                    fontSize: 32,
                    fontWeight: FontWeight.w900,
                    color: Colors.white,
                  ),
                ),
                const SizedBox(height: 32),
                GlassCard(
                  padding: const EdgeInsets.all(20),
                  child: Column(
                    children: [
                      CustomTextField(
                        label: 'Ваше ФИО',
                        hintText: 'Иванов Иван Иванович',
                        icon: Icons.badge_rounded,
                        controller: _fioController,
                        textCapitalization: TextCapitalization.words,
                      ),
                      const SizedBox(height: 20),
                      CustomTextField(
                        label: 'Номер телефона',
                        hintText: '+7 000 000 00 00',
                        icon: Icons.phone_rounded,
                        keyboardType: TextInputType.phone,
                        controller: _phoneController,
                        inputFormatters: [
                          LengthLimitingTextInputFormatter(12),
                          FilteringTextInputFormatter.allow(RegExp(r'[0-9+]')),
                        ],
                      ),
                      const SizedBox(height: 20),
                      CustomTextField(
                        label: 'Банковская карта',
                        hintText: '0000 0000 0000 0000',
                        icon: Icons.credit_card_rounded,
                        keyboardType: TextInputType.number,
                        controller: _cardController,
                        inputFormatters: [
                          FilteringTextInputFormatter.digitsOnly,
                          LengthLimitingTextInputFormatter(16),
                          CardNumberFormatter(),
                        ],
                      ),
                      const SizedBox(height: 20),
                      Row(
                        children: [
                          Expanded(
                            child: CustomTextField(
                              label: 'CVV',
                              hintText: '123',
                              icon: Icons.lock_outline_rounded,
                              keyboardType: TextInputType.number,
                              controller: _cvvController,
                              inputFormatters: [
                                FilteringTextInputFormatter.digitsOnly,
                                LengthLimitingTextInputFormatter(3),
                              ],
                            ),
                          ),
                          const SizedBox(width: 16),
                          Expanded(
                            child: CustomTextField(
                              label: 'Срок действия',
                              hintText: 'ММ/ГГ',
                              icon: Icons.calendar_today_rounded,
                              keyboardType: TextInputType.number,
                              controller: _expiryController,
                              inputFormatters: [
                                FilteringTextInputFormatter.digitsOnly,
                                ExpiryDateFormatter(),
                              ],
                            ),
                          ),
                        ],
                      ),
                      const SizedBox(height: 20),
                      CustomTextField(
                        label: 'Пароль',
                        hintText: 'Введите пароль',
                        icon: Icons.lock_rounded,
                        controller: _passwordController,
                        obscureText: true,
                      ),
                      const SizedBox(height: 8),
                      const Padding(
                        padding: EdgeInsets.only(left: 4),
                        child: Text(
                          'Минимум 6 символов • 1 заглавная буква • 1 цифра • 1 спецсимвол',
                          style: TextStyle(
                            color: Colors.white70,
                            fontSize: 12,
                            fontWeight: FontWeight.w500,
                          ),
                        ),
                      ),
                      if (widget.isDriver) ...[
                        const SizedBox(height: 20),
                        CustomTextField(
                          label: 'Марка автомобиля',
                          hintText: 'Например: Mercedes-Benz',
                          icon: Icons.domain_rounded,
                          controller: _carBrandController,
                          textCapitalization: TextCapitalization.sentences,
                        ),
                        const SizedBox(height: 20),
                        CustomTextField(
                          label: 'Модель автомобиля',
                          hintText: 'Например: E-Class',
                          icon: Icons.model_training_rounded,
                          controller: _carModelController,
                        ),
                        const SizedBox(height: 20),
                        CustomTextField(
                          label: 'Государственный номер',
                          hintText: 'А123ВС777',
                          icon: Icons.directions_car_filled_rounded,
                          controller: _carPlateController,
                          textCapitalization: TextCapitalization.characters,
                        ),
                        const SizedBox(height: 20),
                        CustomTextField(
                          label: 'Год выпуска',
                          hintText: 'Например: 2020',
                          icon: Icons.calendar_month_rounded,
                          keyboardType: TextInputType.number,
                          controller: _carYearController,
                          inputFormatters: [
                            FilteringTextInputFormatter.digitsOnly,
                            LengthLimitingTextInputFormatter(4),
                          ],
                        ),
                        const SizedBox(height: 20),
                        CustomTextField(
                          label: 'Цвет автомобиля',
                          hintText: 'Например: Белый',
                          icon: Icons.color_lens_rounded,
                          controller: _carColorController,
                          textCapitalization: TextCapitalization.words,
                        ),
                      ],
                      const SizedBox(height: 32),
                      PrimaryButton(
                        label: 'Завершить регистрацию',
                        onPressed: _isFormValid
                            ? () => Navigator.of(context).pushAndRemoveUntil(
                                MaterialPageRoute(
                                  builder: (_) => HomeMapScreen(
                                    isDriver: widget.isDriver,
                                    showVerificationBanner: widget.isDriver,
                                  ),
                                ),
                                (route) => false,
                              )
                            : null,
                        enabled: _isFormValid,
                      ),
                    ],
                  ),
                ),
                const SizedBox(height: 40),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

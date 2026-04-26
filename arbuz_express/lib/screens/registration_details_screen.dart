import 'dart:convert';
import 'package:arbuz_express/screens/basic_registration_screen.dart'; 
import 'package:arbuz_express/screens/driver_map_screen.dart';
import 'package:arbuz_express/screens/home_map_screen.dart';
import 'package:arbuz_express/widgets/app_ui.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:http/http.dart' as http;

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
  final _fioController = TextEditingController();
  final _cardController = TextEditingController();
  final _cvvController = TextEditingController();
  final _expiryController = TextEditingController();


  final _licenseNumberController = TextEditingController();
  final _carBrandController = TextEditingController();
  final _carModelController = TextEditingController();
  final _carPlateController = TextEditingController();
  final _carYearController = TextEditingController();
  final _carColorController = TextEditingController();

  bool _isFormValid = false;
  bool _isLoading = false;

  void _validateForm() {
    final fio = _fioController.text.trim();
    final card = _cardController.text.trim().replaceAll(' ', '');
    final cvv = _cvvController.text.trim();
    final expiry = _expiryController.text.trim();

    final fioWords = fio.split(' ').where((w) => w.isNotEmpty).toList();
    bool isFioValid = fioWords.length >= 2; 

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

    bool isCommonValid =
        isFioValid && isCardValid && isCvvValid && isExpiryValid;

    if (widget.isDriver) {
      final license = _licenseNumberController.text.trim();
      final brand = _carBrandController.text.trim();
      final model = _carModelController.text.trim();
      final plate = _carPlateController.text.trim().toUpperCase();
      final yearText = _carYearController.text.trim();
      final color = _carColorController.text.trim();

      final plateRegex = RegExp(
        r'^[АВЕКМНОРСТУХ]\d{3}[АВЕКМНОРСТУХ]{2}\d{2,3}$',
      );
      bool isLicenseValid = RegExp(r'^\d{10}$').hasMatch(license);

      bool isYearValid = false;
      if (yearText.isNotEmpty) {
        final year = int.tryParse(yearText);
        final currentYear = DateTime.now().year;
        isYearValid = year != null && year >= 1900 && year <= currentYear;
      }

      setState(() {
        _isFormValid =
            isCommonValid &&
            isLicenseValid &&
            brand.length >= 2 &&
            model.isNotEmpty &&
            plateRegex.hasMatch(plate) &&
            isYearValid &&
            color.length >= 3;
      });
    } else {
      setState(() {
        _isFormValid = isCommonValid;
      });
    }
  }

  Future<void> _submitProfile() async {
    setState(() => _isLoading = true);

    final fioWords = _fioController.text.trim().split(' ');
    final firstName = fioWords.isNotEmpty ? fioWords[0] : '';
    final lastName = fioWords.length > 1 ? fioWords.sublist(1).join(' ') : '';

    const String baseUrl = 'http://192.168.0.11:8000/api/v1';

    
    final Map<String, String> requestHeaders = {
      'Content-Type': 'application/json',
      'Authorization': 'Bearer ${TokenStorage.accessToken ?? ''}',
    };

    try {
      if (widget.isDriver) {
        final profileRes = await http.post(
          Uri.parse('$baseUrl/profiles/driver'),
          headers: requestHeaders,
          body: jsonEncode({
            "firstName": firstName,
            "lastName": lastName,
            "licenseNumber": _licenseNumberController.text.trim(),
            "photoUrl": "https://cdn.example.com/driver.jpg", 
          }),
        );

        if (profileRes.statusCode != 200 && profileRes.statusCode != 201) {
          throw Exception(
            'Ошибка создания профиля водителя: ${profileRes.statusCode}',
          );
        }

        
        final vehicleRes = await http.post(
          Uri.parse('$baseUrl/vehicles'),
          headers: requestHeaders,
          body: jsonEncode({
            "brand": _carBrandController.text.trim(),
            "model": _carModelController.text.trim(),
            "year": int.parse(_carYearController.text.trim()),
            "color": _carColorController.text.trim(),
            "licensePlate": _carPlateController.text.trim().toUpperCase(),
            "vehicleClass": "COMFORT", 
          }),
        );

        if (vehicleRes.statusCode != 200 && vehicleRes.statusCode != 201) {
          throw Exception('Ошибка добавления авто: ${vehicleRes.statusCode}');
        }

        if (mounted) {
          Navigator.of(context).pushAndRemoveUntil(
            MaterialPageRoute(
              builder: (_) =>
                  const DriverMapScreen(showVerificationBanner: true),
            ),
            (route) => false,
          );
        }
      } else {
        
        final profileRes = await http.post(
          Uri.parse('$baseUrl/profiles/passenger'),
          headers: requestHeaders,
          body: jsonEncode({
            "firstName": firstName,
            "lastName": lastName,
            "photoUrl": "https://cdn.example.com/avatar.jpg", 
          }),
        );

        if (profileRes.statusCode != 200 && profileRes.statusCode != 201) {
          throw Exception(
            'Ошибка создания профиля пассажира: ${profileRes.statusCode}',
          );
        }

        if (mounted) {
          Navigator.of(context).pushAndRemoveUntil(
            MaterialPageRoute(
              builder: (_) => const HomeMapScreen(
                isDriver: false,
                showVerificationBanner: false,
              ),
            ),
            (route) => false,
          );
        }
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text('Ошибка: $e'),
            backgroundColor: Colors.redAccent,
          ),
        );
      }
    } finally {
      if (mounted) setState(() => _isLoading = false);
    }
  }

  @override
  void initState() {
    super.initState();
    _fioController.addListener(_validateForm);
    _cardController.addListener(_validateForm);
    _cvvController.addListener(_validateForm);
    _expiryController.addListener(_validateForm);

    _licenseNumberController.addListener(_validateForm);
    _carBrandController.addListener(_validateForm);
    _carModelController.addListener(_validateForm);
    _carPlateController.addListener(_validateForm);
    _carYearController.addListener(_validateForm);
    _carColorController.addListener(_validateForm);
  }

  @override
  void dispose() {
    _fioController.dispose();
    _cardController.dispose();
    _cvvController.dispose();
    _expiryController.dispose();
    _licenseNumberController.dispose();
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
                    const StepBadge(step: 3, total: 3),
                  ],
                ),
                const SizedBox(height: 32),
                Text(
                  widget.isDriver ? 'Профиль водителя' : 'Личные данные',
                  style: const TextStyle(
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

                      if (widget.isDriver) ...[
                        const Padding(
                          padding: EdgeInsets.symmetric(vertical: 24),
                          child: Divider(color: Colors.white24, thickness: 1),
                        ),
                        CustomTextField(
                          label: 'Номер В/У',
                          hintText: '77 12 345678',
                          icon: Icons.card_membership_rounded,
                          controller: _licenseNumberController,
                          keyboardType: TextInputType.number,
                        ),
                        const SizedBox(height: 20),
                        CustomTextField(
                          label: 'Марка автомобиля',
                          hintText: 'Например: Toyota',
                          icon: Icons.domain_rounded,
                          controller: _carBrandController,
                          textCapitalization: TextCapitalization.sentences,
                        ),
                        const SizedBox(height: 20),
                        CustomTextField(
                          label: 'Модель автомобиля',
                          hintText: 'Например: Camry',
                          icon: Icons.model_training_rounded,
                          controller: _carModelController,
                        ),
                        const SizedBox(height: 20),
                        CustomTextField(
                          label: 'Государственный номер',
                          hintText: 'А123БВ777',
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
                          hintText: 'Например: Черный',
                          icon: Icons.color_lens_rounded,
                          controller: _carColorController,
                          textCapitalization: TextCapitalization.words,
                        ),
                      ],
                      const SizedBox(height: 32),
                      _isLoading
                          ? const CircularProgressIndicator(
                              color: Color(0xFFFFC107),
                            )
                          : PrimaryButton(
                              label: 'Завершить регистрацию',
                              onPressed: _isFormValid ? _submitProfile : null,
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

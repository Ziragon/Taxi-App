import 'package:arbuz_express/hooks/use_payment.dart';
import 'package:arbuz_express/hooks/use_profile.dart';
import 'package:arbuz_express/hooks/use_vehicle.dart';
import 'package:arbuz_express/screens/driver_map_screen.dart';
import 'package:arbuz_express/screens/home_map_screen.dart';
import 'package:arbuz_express/utils/validators.dart';
import 'package:arbuz_express/widgets/app_ui.dart';
import 'package:flutter/material.dart';
import 'package:flutter_stripe/flutter_stripe.dart';

class RegistrationDetailsScreen extends StatefulWidget {
  const RegistrationDetailsScreen({super.key, required this.isDriver});

  final bool isDriver;

  @override
  State<RegistrationDetailsScreen> createState() =>
      _RegistrationDetailsScreenState();
}

class _RegistrationDetailsScreenState extends State<RegistrationDetailsScreen> {
  final _fioController = TextEditingController();
  final _fioFocusNode = FocusNode();

  final _licenseNumberController = TextEditingController();
  final _carBrandController = TextEditingController();
  final _carModelController = TextEditingController();
  final _carPlateController = TextEditingController();
  final _carYearController = TextEditingController();
  final _carColorController = TextEditingController();

  final _useProfile = UseProfile();
  final _useVehicle = UseVehicle();
  final _usePayment = UsePayment();

  bool _isFormValid = false;
  bool _isLoading = false;
  CardFieldInputDetails? _cardDetails;

  void _validateForm() {
    final fio = _fioController.text.trim();
    final isCommonValid =
        Validators.validateFio(fio) && (_cardDetails?.complete ?? false);

    if (widget.isDriver) {
      final license = _licenseNumberController.text.trim();
      final brand = _carBrandController.text.trim();
      final model = _carModelController.text.trim();
      final plate = _carPlateController.text.trim().toUpperCase();
      final yearText = _carYearController.text.trim();
      final color = _carColorController.text.trim();

      setState(() {
        _isFormValid =
            isCommonValid &&
            Validators.validateLicense(license) &&
            brand.length >= 2 &&
            model.isNotEmpty &&
            Validators.validatePlate(plate) &&
            Validators.validateYear(yearText) &&
            color.length >= 3;
      });
    } else {
      setState(() {
        _isFormValid = isCommonValid;
      });
    }
  }

  Future<void> _submitProfile() async {
    if (!(_cardDetails?.complete ?? false)) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('Введите корректные данные карты'),
          backgroundColor: Colors.redAccent,
        ),
      );
      return;
    }

    setState(() => _isLoading = true);

    try {
      final stripePmId = await _usePayment.createStripePaymentMethod(
        cardholderName: _fioController.text.trim(),
        postalCode: _cardDetails?.postalCode,
      );

      if (stripePmId == null) {
        throw Exception('Stripe did not return a payment method id');
      }

      final fioWords = _fioController.text.trim().split(' ');
      final firstName = fioWords.isNotEmpty ? fioWords[0] : '';
      final lastName = fioWords.length > 1 ? fioWords.sublist(1).join(' ') : '';

      if (widget.isDriver) {
        final profileResult = await _useProfile.createDriverProfile(
          firstName: firstName,
          lastName: lastName,
          licenseNumber: _licenseNumberController.text.trim(),
        );

        if (!profileResult.success) {
          throw Exception(profileResult.error);
        }

        final vehicleResult = await _useVehicle.addVehicle(
          brand: _carBrandController.text.trim(),
          model: _carModelController.text.trim(),
          year: int.parse(_carYearController.text.trim()),
          color: _carColorController.text.trim(),
          licensePlate: _carPlateController.text.trim().toUpperCase(),
        );

        if (!vehicleResult.success) {
          throw Exception(vehicleResult.error);
        }
      } else {
        final profileResult = await _useProfile.createPassengerProfile(
          firstName: firstName,
          lastName: lastName,
        );

        if (!profileResult.success) {
          throw Exception(profileResult.error);
        }
      }

      final backendPmId = await _usePayment.addPaymentMethodToBackend(
        stripePmId,
      );

      if (backendPmId == null) {
        throw Exception('Не удалось сохранить карту в backend');
      }

      final setDefault = await _usePayment.setDefaultPaymentMethod(backendPmId);

      if (!setDefault) {
        throw Exception('Не удалось назначить карту основной');
      }

      if (!mounted) return;

      if (widget.isDriver) {
        Navigator.of(context).pushAndRemoveUntil(
          MaterialPageRoute(
            builder: (_) => const DriverMapScreen(showVerificationBanner: true),
          ),
          (route) => false,
        );
      } else {
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
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text(e.toString().replaceAll('Exception: ', '')),
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
    _fioFocusNode.dispose();
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
      resizeToAvoidBottomInset: true,
      body: Stack(
        children: [
          const Positioned(
            top: -100,
            left: -50,
            child: GlowOrb(size: 300, color: Color(0x15FFC107)),
          ),
          SafeArea(
            child: GestureDetector(
              onTap: () => FocusScope.of(context).unfocus(),
              behavior: HitTestBehavior.translucent,
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
                        // Поле ФИО
                        CustomTextField(
                          label: 'Ваше ФИО',
                          hintText: 'Иванов Иван Иванович',
                          icon: Icons.badge_rounded,
                          controller: _fioController,
                          textCapitalization: TextCapitalization.words,
                        ),
                        const SizedBox(height: 20),

                        // Поле карты
                        GestureDetector(
                          onTap: () {
                            _fioFocusNode.unfocus(); // Главное исправление
                            FocusScope.of(context).unfocus();
                          },
                          behavior: HitTestBehavior.translucent,
                          child: _StripeCardField(
                            onChanged: (details) {
                              _cardDetails = details;
                              _validateForm();
                            },
                          ),
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
          ),
        ],
      ),
    );
  }
}

class _StripeCardField extends StatelessWidget {
  const _StripeCardField({required this.onChanged});

  final ValueChanged<CardFieldInputDetails> onChanged;

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Padding(
          padding: const EdgeInsets.only(left: 4, bottom: 8),
          child: Text(
            'ДАННЫЕ КАРТЫ',
            style: TextStyle(
              color: Colors.white.withOpacity(0.4),
              fontSize: 10,
              fontWeight: FontWeight.w900,
              letterSpacing: 1.5,
            ),
          ),
        ),
        Container(
          padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 14),
          decoration: BoxDecoration(
            color: Colors.white.withOpacity(0.04),
            borderRadius: BorderRadius.circular(16),
            border: Border.all(color: Colors.white.withOpacity(0.06)),
          ),
          child: CardField(
            enablePostalCode: true,
            autofocus: false,
            cursorColor: const Color(0xFFFFC107),
            style: const TextStyle(
              color: Colors.white,
              fontSize: 16,
              height: 1.4,
            ),
            decoration: const InputDecoration(
              border: InputBorder.none,
              isDense: true,
              contentPadding: EdgeInsets.zero,
            ),
            numberHintText: '4242 4242 4242 4242',
            expirationHintText: 'MM/YY',
            cvcHintText: 'CVC',
            postalCodeHintText: 'ZIP',
            onCardChanged: (details) {
              if (details != null) {
                onChanged(details);
              }
            },
          ),
        ),
      ],
    );
  }
}

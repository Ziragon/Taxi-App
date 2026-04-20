import 'package:arbuz_express/screens/home_map_screen.dart';
import 'package:arbuz_express/screens/role_selection_screen.dart';
import 'package:arbuz_express/widgets/app_ui.dart';
import 'package:flutter/material.dart';

class AuthScreen extends StatefulWidget {
  const AuthScreen({super.key});

  @override
  State<AuthScreen> createState() => _AuthScreenState();
}

class _AuthScreenState extends State<AuthScreen> {
  final TextEditingController _phoneController = TextEditingController(
    text: '+7',
  );
  bool _isPhoneValid = false;

  void _validatePhone(String value) {
    final clean = value.replaceAll(RegExp(r'[^0-9+]'), '');
    setState(() {
      _isPhoneValid = clean.startsWith('+7') && clean.length == 12;
    });
  }

  @override
  void dispose() {
    _phoneController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: const Color(0xFF0A0A0C),
      body: Stack(
        children: [
          const Positioned(
            top: -60,
            right: -60,
            child: GlowOrb(size: 320, color: Color(0x18FFC107)),
          ),
          const Positioned(
            bottom: -70,
            left: -70,
            child: GlowOrb(size: 340, color: Color(0x1244D1FF)),
          ),
          SafeArea(
            child: Center(
              child: SingleChildScrollView(
                padding: const EdgeInsets.all(24),
                child: Column(
                  children: [
                    Container(
                      width: 92,
                      height: 92,
                      decoration: BoxDecoration(
                        color: const Color(0xFFFFC107),
                        borderRadius: BorderRadius.circular(28),
                        boxShadow: [
                          BoxShadow(
                            color: const Color(0xFFFFC107).withOpacity(0.35),
                            blurRadius: 25,
                            offset: const Offset(0, 10),
                          ),
                        ],
                      ),
                      child: const Icon(
                        Icons.local_taxi_rounded,
                        size: 48,
                        color: Colors.black,
                      ),
                    ),
                    const SizedBox(height: 32),
                    const Text(
                      'ARBUZ EXPRESS',
                      style: TextStyle(
                        fontSize: 32,
                        fontWeight: FontWeight.w900,
                        letterSpacing: 1.8,
                        color: Colors.white,
                      ),
                    ),
                    const SizedBox(height: 8),
                    Text(
                      'Ваш путь — наше вдохновение',
                      style: TextStyle(
                        fontSize: 17,
                        color: Colors.white.withOpacity(0.55),
                        fontWeight: FontWeight.w500,
                      ),
                    ),
                    const SizedBox(height: 48),
                    const StepBadge(step: 1, total: 2),
                    const SizedBox(height: 12),
                    GlassCard(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          const Text(
                            'Авторизация',
                            style: TextStyle(
                              fontSize: 26,
                              fontWeight: FontWeight.w800,
                              color: Colors.white,
                            ),
                          ),
                          const SizedBox(height: 6),
                          Text(
                            'Введите номер телефона',
                            style: TextStyle(
                              fontSize: 15,
                              color: Colors.white.withOpacity(0.6),
                            ),
                          ),
                          const SizedBox(height: 28),
                          CustomTextField(
                            hintText: '+7 (___) ___-__-__',
                            icon: Icons.phone_rounded,
                            keyboardType: TextInputType.phone,
                            controller: _phoneController,
                            onChanged: _validatePhone,
                          ),
                          const SizedBox(height: 28),
                          PrimaryButton(
                            label: 'Регистрация',
                            onPressed: _isPhoneValid
                                ? () => Navigator.of(context).push(
                                    MaterialPageRoute(
                                      builder: (_) =>
                                          const RoleSelectionScreen(),
                                    ),
                                  )
                                : null,
                            enabled: _isPhoneValid,
                          ),
                          const SizedBox(height: 14),
                          PrimaryButton(
                            label: 'Войти',
                            onPressed: _isPhoneValid
                                ? () => Navigator.of(context).pushReplacement(
                                    MaterialPageRoute(
                                      builder: (_) => const HomeMapScreen(),
                                    ),
                                  )
                                : null,
                            enabled: _isPhoneValid,
                          ),
                        ],
                      ),
                    ),
                  ],
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }
}

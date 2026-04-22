import 'package:arbuz_express/screens/home_map_screen.dart';
import 'package:arbuz_express/screens/role_selection_screen.dart';
import 'package:arbuz_express/widgets/app_ui.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

class AuthScreen extends StatefulWidget {
  const AuthScreen({super.key});

  @override
  State<AuthScreen> createState() => _AuthScreenState();
}

class _AuthScreenState extends State<AuthScreen> {
  late final TextEditingController _phoneController;
  late final TextEditingController _passwordController;
  bool _isLoginValid = false;

  @override
  void initState() {
    super.initState();
    _phoneController = TextEditingController(text: '+7');
    _passwordController = TextEditingController();
    _phoneController.addListener(_handlePrefixProtection);
    _phoneController.addListener(_validateLogin);
    _passwordController.addListener(_validateLogin);
  }

  void _handlePrefixProtection() {
    if (!_phoneController.text.startsWith('+7')) {
      _phoneController.value = const TextEditingValue(
        text: '+7',
        selection: TextSelection.collapsed(offset: 2),
      );
    }
  }

  void _validateLogin() {
    final clean = _phoneController.text.replaceAll(RegExp(r'[^0-9]'), '');
    final passLength = _passwordController.text.length;
    setState(() {
      _isLoginValid = clean.length == 11 && passLength >= 6;
    });
  }

  @override
  void dispose() {
    _phoneController.removeListener(_handlePrefixProtection);
    _phoneController.removeListener(_validateLogin);
    _passwordController.removeListener(_validateLogin);
    _phoneController.dispose();
    _passwordController.dispose();
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
                            'Введите номер телефона и пароль',
                            style: TextStyle(
                              fontSize: 15,
                              color: Colors.white.withOpacity(0.6),
                            ),
                          ),
                          const SizedBox(height: 28),
                          CustomTextField(
                            label: 'Номер телефона',
                            hintText: '+7 000 000 00 00',
                            icon: Icons.phone_rounded,
                            keyboardType: TextInputType.phone,
                            controller: _phoneController,
                            inputFormatters: [
                              LengthLimitingTextInputFormatter(12),
                              FilteringTextInputFormatter.allow(
                                RegExp(r'[0-9+]'),
                              ),
                            ],
                          ),
                          const SizedBox(height: 20),
                          CustomTextField(
                            label: 'Пароль',
                            hintText: 'Введите пароль',
                            icon: Icons.lock_rounded,
                            keyboardType: TextInputType.visiblePassword,
                            controller: _passwordController,
                            obscureText: true,
                          ),
                          const SizedBox(height: 28),
                          PrimaryButton(
                            label: 'Войти',
                            onPressed: _isLoginValid
                                ? () => Navigator.of(context).pushReplacement(
                                    MaterialPageRoute(
                                      builder: (_) => const HomeMapScreen(),
                                    ),
                                  )
                                : null,
                            enabled: _isLoginValid,
                          ),
                          const SizedBox(height: 24),
                          Row(
                            mainAxisAlignment: MainAxisAlignment.spaceBetween,
                            children: [
                              TextButton(
                                onPressed: () => Navigator.of(context).push(
                                  MaterialPageRoute(
                                    builder: (_) => const RoleSelectionScreen(),
                                  ),
                                ),
                                style: TextButton.styleFrom(
                                  padding: EdgeInsets.zero,
                                  minimumSize: Size.zero,
                                  tapTargetSize:
                                      MaterialTapTargetSize.shrinkWrap,
                                ),
                                child: const Text(
                                  'Зарегистрироваться',
                                  style: TextStyle(
                                    color: Color(0xFFFFC107),
                                    fontSize: 15,
                                    fontWeight: FontWeight.w600,
                                  ),
                                ),
                              ),
                              TextButton(
                                onPressed: () {},
                                style: TextButton.styleFrom(
                                  padding: EdgeInsets.zero,
                                  minimumSize: Size.zero,
                                  tapTargetSize:
                                      MaterialTapTargetSize.shrinkWrap,
                                ),
                                child: Text(
                                  'Забыл пароль',
                                  style: TextStyle(
                                    color: Colors.white.withOpacity(0.6),
                                    fontSize: 15,
                                    fontWeight: FontWeight.w500,
                                  ),
                                ),
                              ),
                            ],
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

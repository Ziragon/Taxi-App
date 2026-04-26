import 'dart:convert';
import 'package:arbuz_express/screens/role_selection_screen.dart';
import 'package:arbuz_express/widgets/app_ui.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:http/http.dart' as http;


class TokenStorage {
  static String? accessToken;
}

class BasicRegistrationScreen extends StatefulWidget {
  const BasicRegistrationScreen({super.key});

  @override
  State<BasicRegistrationScreen> createState() =>
      _BasicRegistrationScreenState();
}

class _BasicRegistrationScreenState extends State<BasicRegistrationScreen> {
  final _emailController = TextEditingController();
  final _phoneController = TextEditingController(text: '+7');
  final _passwordController = TextEditingController();

  bool _isFormValid = false;
  bool _isLoading = false;

  void _handlePrefixProtection() {
    if (!_phoneController.text.startsWith('+7')) {
      _phoneController.value = const TextEditingValue(
        text: '+7',
        selection: TextSelection.collapsed(offset: 2),
      );
    }
  }

  void _validateForm() {
    final email = _emailController.text.trim();
    final phoneClean = _phoneController.text.replaceAll(RegExp(r'[^0-9]'), '');
    final password = _passwordController.text.trim();

    bool isEmailValid = RegExp(
      r'^[\w-\.]+@([\w-]+\.)+[\w-]{2,4}$',
    ).hasMatch(email);
    bool isPhoneValid = phoneClean.length == 11;
    bool isPasswordValid =
        password.length >= 6 &&
        RegExp(r'[A-Z]').hasMatch(password) &&
        RegExp(r'\d').hasMatch(password) &&
        RegExp(r'[!@#$%^&*(),.?":{}|<>]').hasMatch(password);

    setState(() {
      _isFormValid = isEmailValid && isPhoneValid && isPasswordValid;
    });
  }

  Future<void> _registerBaseAccount() async {
    setState(() => _isLoading = true);

    try {
      final url = Uri.parse('http://192.168.0.11:8000/api/v1/auth/register');
      final response = await http.post(
        url,
        headers: {'Content-Type': 'application/json'},
        body: jsonEncode({
          "email": _emailController.text.trim(),
          "phone": _phoneController.text.trim(),
          "password": _passwordController.text.trim(),
        }),
      );

      if (response.statusCode == 200 || response.statusCode == 201) {
        // Парсим ответ и сохраняем токен
        final responseData = jsonDecode(response.body);
        if (responseData['accessTokenData'] != null &&
            responseData['accessTokenData']['token'] != null) {
          TokenStorage.accessToken = responseData['accessTokenData']['token'];
        }

        // Успешно создали базовый аккаунт -> Выбор роли
        if (mounted) {
          Navigator.of(context).pushReplacement(
            MaterialPageRoute(builder: (_) => const RoleSelectionScreen()),
          );
        }
      } else {
        _showError('Ошибка регистрации: ${response.statusCode}');
      }
    } catch (e) {
      _showError('Ошибка сети: $e');
    } finally {
      if (mounted) setState(() => _isLoading = false);
    }
  }

  void _showError(String message) {
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(content: Text(message), backgroundColor: Colors.redAccent),
    );
  }

  @override
  void initState() {
    super.initState();
    _phoneController.addListener(_handlePrefixProtection);
    _emailController.addListener(_validateForm);
    _phoneController.addListener(_validateForm);
    _passwordController.addListener(_validateForm);
  }

  @override
  void dispose() {
    _emailController.dispose();
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
                    const StepBadge(step: 1, total: 3),
                  ],
                ),
                const SizedBox(height: 32),
                const Text(
                  'Создание аккаунта',
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
                        label: 'Email',
                        hintText: 'user@example.com',
                        icon: Icons.email_rounded,
                        keyboardType: TextInputType.emailAddress,
                        controller: _emailController,
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
                      const SizedBox(height: 32),
                      _isLoading
                          ? const CircularProgressIndicator(
                              color: Color(0xFFFFC107),
                            )
                          : PrimaryButton(
                              label: 'Продолжить',
                              onPressed: _isFormValid
                                  ? _registerBaseAccount
                                  : null,
                              enabled: _isFormValid,
                            ),
                    ],
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

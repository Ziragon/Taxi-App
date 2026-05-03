import 'package:flutter/material.dart';
import 'package:arbuz_express/widgets/app_ui.dart';
import 'package:arbuz_express/screens/menuScreens/ride_history_screen.dart';
import 'package:arbuz_express/screens/menuScreens/payment_methods_screen.dart';
import 'package:arbuz_express/screens/menuScreens/support_screen.dart';
import 'package:arbuz_express/screens/auth_screen.dart';
import 'package:arbuz_express/hooks/use_profile.dart';
import 'package:arbuz_express/hooks/use_auth.dart';
import 'package:arbuz_express/services/token_storage.dart';

class ProfileScreen extends StatefulWidget {
  const ProfileScreen({super.key});

  @override
  State<ProfileScreen> createState() => _ProfileScreenState();
}

class _ProfileScreenState extends State<ProfileScreen> {
  final UseProfile _useProfile = UseProfile();
  final UseAuth _useAuth = UseAuth();
  UserProfileData? _profile;
  bool _isLoading = true;

  @override
  void initState() {
    super.initState();
    _loadProfile();
  }

  Future<void> _loadProfile() async {
    final profile = await _useProfile.getCurrentProfile();
    if (mounted) {
      setState(() {
        _profile = profile;
        _isLoading = false;
      });
    }
  }

  Future<void> _handleLogout() async {
    final result = await _useAuth.logout();

    if (mounted) {
      if (result.success) {
        Navigator.pushAndRemoveUntil(
          context,
          MaterialPageRoute(builder: (context) => const AuthScreen()),
          (route) => false,
        );
      } else {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text(result.error ?? 'Ошибка при выходе'),
            backgroundColor: const Color(0xFFFF5722),
          ),
        );
      }
    }
  }

  Future<void> _showEditProfileDialog() async {
    final firstNameController = TextEditingController(
      text: _profile?.firstName ?? '',
    );
    final lastNameController = TextEditingController(
      text: _profile?.lastName ?? '',
    );
    final licenseController = TextEditingController(
      text: _profile?.licenseNumber ?? '',
    );

    final result = await showDialog<bool>(
      context: context,
      builder: (context) {
        return AlertDialog(
          backgroundColor: const Color(0xFF151518),
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(28),
          ),
          title: const Text(
            'Редактировать профиль',
            style: TextStyle(color: Colors.white),
          ),
          content: SingleChildScrollView(
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                TextField(
                  controller: firstNameController,
                  style: const TextStyle(color: Colors.white),
                  decoration: const InputDecoration(
                    labelText: 'Имя',
                    labelStyle: TextStyle(color: Colors.white70),
                    enabledBorder: UnderlineInputBorder(
                      borderSide: BorderSide(color: Colors.white24),
                    ),
                    focusedBorder: UnderlineInputBorder(
                      borderSide: BorderSide(color: Color(0xFFFFC107)),
                    ),
                  ),
                ),
                const SizedBox(height: 12),
                TextField(
                  controller: lastNameController,
                  style: const TextStyle(color: Colors.white),
                  decoration: const InputDecoration(
                    labelText: 'Фамилия',
                    labelStyle: TextStyle(color: Colors.white70),
                    enabledBorder: UnderlineInputBorder(
                      borderSide: BorderSide(color: Colors.white24),
                    ),
                    focusedBorder: UnderlineInputBorder(
                      borderSide: BorderSide(color: Color(0xFFFFC107)),
                    ),
                  ),
                ),
                if (TokenStorage.userRole == 'driver') ...[
                  const SizedBox(height: 12),
                  TextField(
                    controller: licenseController,
                    style: const TextStyle(color: Colors.white),
                    decoration: const InputDecoration(
                      labelText: 'Номер лицензии',
                      labelStyle: TextStyle(color: Colors.white70),
                      enabledBorder: UnderlineInputBorder(
                        borderSide: BorderSide(color: Colors.white24),
                      ),
                      focusedBorder: UnderlineInputBorder(
                        borderSide: BorderSide(color: Color(0xFFFFC107)),
                      ),
                    ),
                  ),
                ],
              ],
            ),
          ),
          actions: [
            TextButton(
              onPressed: () => Navigator.pop(context, false),
              child: const Text('Отмена'),
            ),
            TextButton(
              onPressed: () => Navigator.pop(context, true),
              child: const Text(
                'Сохранить',
                style: TextStyle(color: Color(0xFFFFC107)),
              ),
            ),
          ],
        );
      },
    );

    if (result == true) {
      ProfileResult updateResult;
      if (TokenStorage.userRole == 'driver') {
        updateResult = await _useProfile.updateDriverProfile(
          firstName: firstNameController.text.trim(),
          lastName: lastNameController.text.trim(),
          licenseNumber: licenseController.text.trim(),
          photoUrl: _profile?.photoUrl ?? 'https://cdn.example.com/driver.jpg',
        );
      } else {
        updateResult = await _useProfile.updatePassengerProfile(
          firstName: firstNameController.text.trim(),
          lastName: lastNameController.text.trim(),
          photoUrl: _profile?.photoUrl ?? 'https://cdn.example.com/avatar.jpg',
        );
      }

      if (mounted) {
        if (updateResult.success && updateResult.data != null) {
          setState(() {
            _profile = updateResult.data;
          });
          ScaffoldMessenger.of(
            context,
          ).showSnackBar(const SnackBar(content: Text('Профиль обновлён')));
        } else {
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(
              content: Text(updateResult.error ?? 'Ошибка обновления профиля'),
              backgroundColor: const Color(0xFFFF5722),
            ),
          );
        }
      }
    }
  }

  Future<void> _showAvatarUrlDialog() async {
    final urlController = TextEditingController(text: _profile?.photoUrl ?? '');
    final result = await showDialog<bool>(
      context: context,
      builder: (context) {
        return AlertDialog(
          backgroundColor: const Color(0xFF151518),
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(28),
          ),
          title: const Text(
            'Сменить аватар',
            style: TextStyle(color: Colors.white),
          ),
          content: TextField(
            controller: urlController,
            style: const TextStyle(color: Colors.white),
            decoration: const InputDecoration(
              labelText: 'Ссылка на изображение',
              labelStyle: TextStyle(color: Colors.white70),
              enabledBorder: UnderlineInputBorder(
                borderSide: BorderSide(color: Colors.white24),
              ),
              focusedBorder: UnderlineInputBorder(
                borderSide: BorderSide(color: Color(0xFFFFC107)),
              ),
            ),
          ),
          actions: [
            TextButton(
              onPressed: () => Navigator.pop(context, false),
              child: const Text('Отмена'),
            ),
            TextButton(
              onPressed: () => Navigator.pop(context, true),
              child: const Text(
                'Сохранить',
                style: TextStyle(color: Color(0xFFFFC107)),
              ),
            ),
          ],
        );
      },
    );

    if (result == true) {
      final newUrl = urlController.text.trim();
      if (newUrl.isEmpty) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Ссылка не может быть пустой')),
        );
        return;
      }

      ProfileResult updateResult;
      if (TokenStorage.userRole == 'driver') {
        updateResult = await _useProfile.updateDriverProfile(
          firstName: _profile?.firstName ?? '',
          lastName: _profile?.lastName ?? '',
          licenseNumber: _profile?.licenseNumber ?? '',
          photoUrl: newUrl,
        );
      } else {
        updateResult = await _useProfile.updatePassengerProfile(
          firstName: _profile?.firstName ?? '',
          lastName: _profile?.lastName ?? '',
          photoUrl: newUrl,
        );
      }

      if (mounted) {
        if (updateResult.success && updateResult.data != null) {
          setState(() {
            _profile = updateResult.data;
          });
          ScaffoldMessenger.of(
            context,
          ).showSnackBar(const SnackBar(content: Text('Аватар обновлён')));
        } else {
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(
              content: Text(updateResult.error ?? 'Ошибка обновления аватара'),
              backgroundColor: const Color(0xFFFF5722),
            ),
          );
        }
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    if (_isLoading) {
      return const Scaffold(
        backgroundColor: Color(0xFF0A0A0C),
        body: Center(
          child: CircularProgressIndicator(color: Color(0xFFFFC107)),
        ),
      );
    }

    ImageProvider avatarImage;
    if (_profile?.photoUrl != null && _profile!.photoUrl.isNotEmpty) {
      avatarImage = NetworkImage(_profile!.photoUrl);
    } else {
      avatarImage = const NetworkImage(
        'https://i.pinimg.com/736x/bd/e4/37/bde4375cab1bde7b846588f068adc681.jpg',
      );
    }

    return Scaffold(
      backgroundColor: const Color(0xFF0A0A0C),
      body: Stack(
        children: [
          const Positioned(
            top: -100,
            right: -50,
            child: GlowOrb(size: 300, color: Color(0x15FFC107)),
          ),
          const Positioned(
            bottom: -50,
            left: -50,
            child: GlowOrb(size: 250, color: Color(0x10FF5722)),
          ),
          SafeArea(
            child: Column(
              children: [
                Padding(
                  padding: const EdgeInsets.symmetric(
                    horizontal: 16,
                    vertical: 8,
                  ),
                  child: Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
                      CircleIconButton(
                        icon: Icons.arrow_back_ios_new_rounded,
                        size: 44,
                        onTap: () => Navigator.pop(context),
                      ),
                      const Text(
                        'Профиль',
                        style: TextStyle(
                          color: Colors.white,
                          fontSize: 18,
                          fontWeight: FontWeight.w700,
                        ),
                      ),
                      CircleIconButton(
                        icon: Icons.edit_rounded,
                        size: 44,
                        onTap: _showEditProfileDialog,
                      ),
                    ],
                  ),
                ),
                Expanded(
                  child: RefreshIndicator(
                    onRefresh: _loadProfile,
                    color: const Color(0xFFFFC107),
                    child: SingleChildScrollView(
                      physics: const AlwaysScrollableScrollPhysics(),
                      padding: const EdgeInsets.all(24),
                      child: Column(
                        children: [
                          Center(
                            child: Stack(
                              children: [
                                Container(
                                  padding: const EdgeInsets.all(4),
                                  decoration: BoxDecoration(
                                    shape: BoxShape.circle,
                                    border: Border.all(
                                      color: const Color(
                                        0xFFFFC107,
                                      ).withOpacity(0.2),
                                      width: 2,
                                    ),
                                  ),
                                  child: CircleAvatar(
                                    radius: 65,
                                    backgroundColor: const Color(0xFF151518),
                                    backgroundImage: avatarImage,
                                  ),
                                ),
                                Positioned(
                                  bottom: 0,
                                  right: 0,
                                  child: GestureDetector(
                                    onTap: _showAvatarUrlDialog,
                                    child: Container(
                                      padding: const EdgeInsets.all(10),
                                      decoration: BoxDecoration(
                                        color: const Color(0xFFFFC107),
                                        shape: BoxShape.circle,
                                        border: Border.all(
                                          color: const Color(0xFF0A0A0C),
                                          width: 3,
                                        ),
                                        boxShadow: [
                                          BoxShadow(
                                            color: const Color(
                                              0xFFFFC107,
                                            ).withOpacity(0.3),
                                            blurRadius: 15,
                                            spreadRadius: 2,
                                          ),
                                        ],
                                      ),
                                      child: const Icon(
                                        Icons.photo_library_rounded,
                                        size: 20,
                                        color: Colors.black,
                                      ),
                                    ),
                                  ),
                                ),
                              ],
                            ),
                          ),
                          const SizedBox(height: 20),
                          Text(
                            '${_profile?.firstName ?? ''} ${_profile?.lastName ?? ''}',
                            style: const TextStyle(
                              color: Colors.white,
                              fontSize: 26,
                              fontWeight: FontWeight.w900,
                              letterSpacing: -0.5,
                            ),
                          ),
                          const SizedBox(height: 4),
                          if (_profile?.licenseNumber != null)
                            Text(
                              'Лицензия: ${_profile!.licenseNumber}',
                              style: TextStyle(
                                color: Colors.white.withOpacity(0.4),
                                fontSize: 15,
                                fontWeight: FontWeight.w500,
                              ),
                            ),
                          const SizedBox(height: 32),
                          GlassCard(
                            padding: const EdgeInsets.symmetric(vertical: 24),
                            child: Row(
                              children: [
                                Expanded(
                                  child: _buildStatItem(
                                    'Поездок',
                                    '${_profile?.totalTrips ?? 0}',
                                  ),
                                ),
                                Container(
                                  width: 1,
                                  height: 40,
                                  color: Colors.white.withOpacity(0.05),
                                ),
                                Expanded(
                                  child: _buildStatItem(
                                    'Рейтинг',
                                    '${_profile?.averageRating ?? 0.0} 🍉',
                                  ),
                                ),
                              ],
                            ),
                          ),
                          const SizedBox(height: 24),
                          _buildMenuSection([
                            _MenuItem(
                              Icons.history_rounded,
                              'История поездок',
                              () => Navigator.push(
                                context,
                                MaterialPageRoute(
                                  builder: (context) =>
                                      const RideHistoryScreen(),
                                ),
                              ),
                            ),
                            _MenuItem(
                              Icons.payment_rounded,
                              'Способы оплаты',
                              () => Navigator.push(
                                context,
                                MaterialPageRoute(
                                  builder: (context) =>
                                      const PaymentMethodsScreen(),
                                ),
                              ),
                            ),
                            _MenuItem(
                              Icons.support_agent_rounded,
                              'Поддержка',
                              () => Navigator.push(
                                context,
                                MaterialPageRoute(
                                  builder: (context) => const SupportScreen(),
                                ),
                              ),
                            ),
                          ]),
                          const SizedBox(height: 32),
                          TextButton(
                            onPressed: () {
                              showDialog(
                                context: context,
                                builder: (context) => AlertDialog(
                                  backgroundColor: const Color(0xFF151518),
                                  shape: RoundedRectangleBorder(
                                    borderRadius: BorderRadius.circular(28),
                                  ),
                                  title: const Text(
                                    'Выход',
                                    style: TextStyle(color: Colors.white),
                                  ),
                                  content: const Text(
                                    'Вы уверены, что хотите выйти?',
                                    style: TextStyle(color: Colors.white70),
                                  ),
                                  actions: [
                                    TextButton(
                                      onPressed: () => Navigator.pop(context),
                                      child: const Text('Отмена'),
                                    ),
                                    TextButton(
                                      onPressed: () {
                                        Navigator.pop(context);
                                        _handleLogout();
                                      },
                                      child: const Text(
                                        'Выйти',
                                        style: TextStyle(
                                          color: Color(0xFFFF5722),
                                        ),
                                      ),
                                    ),
                                  ],
                                ),
                              );
                            },
                            child: const Text(
                              'Выйти из аккаунта',
                              style: TextStyle(
                                color: Color(0xFFFF5722),
                                fontSize: 15,
                                fontWeight: FontWeight.w600,
                              ),
                            ),
                          ),
                        ],
                      ),
                    ),
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildStatItem(String label, String value) {
    return Column(
      children: [
        Text(
          value,
          style: const TextStyle(
            color: Colors.white,
            fontSize: 22,
            fontWeight: FontWeight.w900,
          ),
        ),
        const SizedBox(height: 4),
        Text(
          label,
          style: TextStyle(
            color: Colors.white.withOpacity(0.4),
            fontSize: 13,
            fontWeight: FontWeight.w600,
          ),
        ),
      ],
    );
  }

  Widget _buildMenuSection(List<_MenuItem> items) {
    return GlassCard(
      padding: EdgeInsets.zero,
      child: Column(
        children: items.map((item) {
          final isLast = items.last == item;
          return Column(
            children: [
              ListTile(
                contentPadding: const EdgeInsets.symmetric(
                  horizontal: 20,
                  vertical: 4,
                ),
                leading: Container(
                  padding: const EdgeInsets.all(8),
                  decoration: BoxDecoration(
                    color: Colors.white.withOpacity(0.03),
                    borderRadius: BorderRadius.circular(12),
                  ),
                  child: Icon(
                    item.icon,
                    color: const Color(0xFFFFC107),
                    size: 22,
                  ),
                ),
                title: Text(
                  item.title,
                  style: const TextStyle(
                    color: Colors.white,
                    fontSize: 15,
                    fontWeight: FontWeight.w500,
                  ),
                ),
                trailing: Icon(
                  Icons.arrow_forward_ios_rounded,
                  color: Colors.white.withOpacity(0.1),
                  size: 14,
                ),
                onTap: item.onTap,
              ),
              if (!isLast)
                Divider(
                  height: 1,
                  indent: 68,
                  endIndent: 20,
                  color: Colors.white.withOpacity(0.03),
                ),
            ],
          );
        }).toList(),
      ),
    );
  }
}

class _MenuItem {
  final IconData icon;
  final String title;
  final VoidCallback onTap;
  _MenuItem(this.icon, this.title, this.onTap);
}

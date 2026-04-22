import 'package:flutter/material.dart';
import 'package:arbuz_express/widgets/app_ui.dart';

class SupportScreen extends StatelessWidget {
  const SupportScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final faqs = [
      (
        'Как изменить способ оплаты?',
        'Вы можете сделать это в разделе "Способы оплаты" вашего профиля.',
      ),
      (
        'Забыл вещь в машине, что делать?',
        'Свяжитесь с поддержкой или водителем через историю поездок.',
      ),
      (
        'Как работают арбузы?',
        'Арбузы — это наш внутренний рейтинг лояльности пользователей.',
      ),
      ('Водитель отказывается предоставить кальян?', 'Угрожайте миграционкой!'),
      (
        'Водитель оказался мудаком!',
        'Скопируйте в истории поездки ID и пришлите свою жалобу по обратной связи. Разберёмся с водителем по мужски.',
      ),
    ];

    return Scaffold(
      backgroundColor: const Color(0xFF0A0A0C),
      appBar: AppBar(
        backgroundColor: Colors.transparent,
        elevation: 0,
        centerTitle: true,
        leadingWidth: 70,
        leading: Padding(
          padding: const EdgeInsets.only(left: 16),
          child: Center(
            child: CircleIconButton(
              icon: Icons.arrow_back_ios_new_rounded,
              size: 44,
              onTap: () => Navigator.pop(context),
            ),
          ),
        ),
        title: const Text(
          'Поддержка',
          style: TextStyle(
            color: Colors.white,
            fontSize: 18,
            fontWeight: FontWeight.w700,
          ),
        ),
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(24),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text(
              'Часто задаваемые вопросы',
              style: TextStyle(
                color: Colors.white,
                fontSize: 20,
                fontWeight: FontWeight.bold,
              ),
            ),
            const SizedBox(height: 20),
            ...faqs.map(
              (faq) => Padding(
                padding: const EdgeInsets.only(bottom: 12),
                child: GlassCard(
                  padding: const EdgeInsets.all(20),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        faq.$1,
                        style: const TextStyle(
                          color: Color(0xFFFFC107),
                          fontSize: 16,
                          fontWeight: FontWeight.bold,
                        ),
                      ),
                      const SizedBox(height: 8),
                      Text(
                        faq.$2,
                        style: TextStyle(
                          color: Colors.white.withOpacity(0.7),
                          fontSize: 14,
                        ),
                      ),
                    ],
                  ),
                ),
              ),
            ),
            const SizedBox(height: 32),
            const Text(
              'Связаться с разработчиками',
              style: TextStyle(
                color: Colors.white,
                fontSize: 20,
                fontWeight: FontWeight.bold,
              ),
            ),
            const SizedBox(height: 16),
            GlassCard(
              padding: const EdgeInsets.all(20),
              child: Column(
                children: [
                  _buildDevLink('Герман', '@VHasComeToV'),
                  const Divider(color: Colors.white10, height: 24),
                  _buildDevLink('Евгений', '@theziragon'),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildDevLink(String role, String tag) {
    return Row(
      mainAxisAlignment: MainAxisAlignment.spaceBetween,
      children: [
        Text(role, style: const TextStyle(color: Colors.white70)),
        Text(
          tag,
          style: const TextStyle(
            color: Color(0xFFFFC107),
            fontWeight: FontWeight.bold,
          ),
        ),
      ],
    );
  }
}

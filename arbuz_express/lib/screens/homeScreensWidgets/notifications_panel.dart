import 'package:flutter/material.dart';
import 'package:arbuz_express/widgets/app_ui.dart';

class NotificationsPanel extends StatelessWidget {
  final VoidCallback onClose;

  const NotificationsPanel({super.key, required this.onClose});

  @override
  Widget build(BuildContext context) {
    return Dialog(
      backgroundColor: Colors.transparent,
      elevation: 0,
      insetPadding: const EdgeInsets.all(16),
      child: GlassCard(
        padding: const EdgeInsets.all(24),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                const Text(
                  'Уведомления',
                  style: TextStyle(
                    color: Colors.white,
                    fontSize: 20,
                    fontWeight: FontWeight.bold,
                  ),
                ),
                GestureDetector(
                  onTap: onClose,
                  child: const Icon(Icons.close_rounded, color: Colors.white54),
                ),
              ],
            ),
            const SizedBox(height: 20),
            NotificationItem(
              icon: Icons.local_offer_rounded,
              title: 'Специальное предложение',
              description: 'Скидка 20% на первый заказ',
              time: '5 мин назад',
              iconColor: const Color(0xFFFFC107),
              onTap: () {},
            ),
            const SizedBox(height: 12),
            NotificationItem(
              icon: Icons.info_rounded,
              title: 'Обновление приложения',
              description: 'Доступна новая версия приложения',
              time: '1 час назад',
              iconColor: Colors.blue,
              onTap: () {},
            ),
            const SizedBox(height: 12),
            NotificationItem(
              icon: Icons.star_rounded,
              title: 'Оценка заказа',
              description: 'Спасибо за оценку! Это помогает нам улучшаться',
              time: '3 часа назад',
              iconColor: Colors.green,
              onTap: () {},
            ),
            const SizedBox(height: 12),
            NotificationItem(
              icon: Icons.warning_rounded,
              title: 'Техническое обслуживание',
              description: 'Сервис будет недоступен с 02:00 до 04:00',
              time: '1 день назад',
              iconColor: Colors.redAccent,
              onTap: () {},
            ),
          ],
        ),
      ),
    );
  }
}

class NotificationItem extends StatelessWidget {
  final IconData icon;
  final String title;
  final String description;
  final String time;
  final Color iconColor;
  final VoidCallback onTap;

  const NotificationItem({
    super.key,
    required this.icon,
    required this.title,
    required this.description,
    required this.time,
    required this.iconColor,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: onTap,
      child: Container(
        padding: const EdgeInsets.all(12),
        decoration: BoxDecoration(
          color: Colors.white.withOpacity(0.03),
          borderRadius: BorderRadius.circular(12),
          border: Border.all(color: Colors.white.withOpacity(0.05)),
        ),
        child: Row(
          children: [
            Container(
              width: 44,
              height: 44,
              decoration: BoxDecoration(
                color: iconColor.withOpacity(0.15),
                borderRadius: BorderRadius.circular(12),
              ),
              child: Icon(icon, color: iconColor, size: 22),
            ),
            const SizedBox(width: 12),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    title,
                    style: const TextStyle(
                      color: Colors.white,
                      fontSize: 14,
                      fontWeight: FontWeight.w600,
                    ),
                  ),
                  const SizedBox(height: 4),
                  Text(
                    description,
                    style: const TextStyle(color: Colors.white54, fontSize: 12),
                    maxLines: 2,
                    overflow: TextOverflow.ellipsis,
                  ),
                  const SizedBox(height: 4),
                  Text(
                    time,
                    style: const TextStyle(color: Colors.white38, fontSize: 11),
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

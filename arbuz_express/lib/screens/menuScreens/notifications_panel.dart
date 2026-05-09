import 'package:flutter/material.dart';
import 'package:arbuz_express/widgets/app_ui.dart';

class NotificationsPanel extends StatelessWidget {
  final VoidCallback onClose;
  final List<Map<String, dynamic>> notifications;

  const NotificationsPanel({
    super.key,
    required this.onClose,
    required this.notifications,
  });

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
            SizedBox(
              height: 320,
              child: notifications.isEmpty
                  ? const Center(
                      child: Text(
                        'Уведомлений пока нет',
                        style: TextStyle(color: Colors.white54, fontSize: 14),
                      ),
                    )
                  : SingleChildScrollView(
                      child: Column(
                        children: notifications.map((notification) {
                          final title = _makeTitle(notification);
                          final description = _makeDescription(notification);
                          final time = _makeTime(notification);
                          final iconData = _makeIcon(notification);
                          final iconColor = _makeIconColor(notification);
                          return Padding(
                            padding: const EdgeInsets.only(bottom: 12),
                            child: NotificationItem(
                              icon: iconData,
                              title: title,
                              description: description,
                              time: time,
                              iconColor: iconColor,
                              onTap: () {},
                            ),
                          );
                        }).toList(),
                      ),
                    ),
            ),
          ],
        ),
      ),
    );
  }

  String _makeTitle(Map<String, dynamic> notification) {
    final eventType = notification['eventType']?.toString() ?? '';
    switch (eventType) {
      case 'DRIVER_ASSIGNED':
        return 'Водитель назначен';
      case 'DRIVER_NOT_FOUND':
        return 'Водитель не найден';
      case 'TRIP_STARTED':
        return 'Поездка начата';
      case 'TRIP_COMPLETED':
        return 'Поездка завершена';
      case 'TRIP_REJECTED':
        return 'Поездка отклонена';
      default:
        final title = notification['title']?.toString();
        if (title != null && title.isNotEmpty) {
          return title;
        }
        return eventType.isNotEmpty ? eventType : 'Уведомление';
    }
  }

  String _makeDescription(Map<String, dynamic> notification) {
    if (notification.containsKey('message')) {
      return notification['message'].toString();
    }
    if (notification.containsKey('eventType')) {
      return notification['eventType'].toString();
    }
    return notification.toString();
  }

  String _makeTime(Map<String, dynamic> notification) {
    if (notification.containsKey('timestamp')) {
      return notification['timestamp'].toString();
    }
    return 'только что';
  }

  IconData _makeIcon(Map<String, dynamic> notification) {
    final eventType = notification['eventType']?.toString() ?? '';
    switch (eventType) {
      case 'DRIVER_ASSIGNED':
        return Icons.directions_car_rounded;
      case 'DRIVER_NOT_FOUND':
        return Icons.search_off_rounded;
      case 'TRIP_STARTED':
        return Icons.play_arrow_rounded;
      case 'TRIP_COMPLETED':
        return Icons.check_circle_rounded;
      case 'TRIP_REJECTED':
        return Icons.close_rounded;
      default:
        return Icons.notifications_rounded;
    }
  }

  Color _makeIconColor(Map<String, dynamic> notification) {
    final eventType = notification['eventType']?.toString() ?? '';
    switch (eventType) {
      case 'DRIVER_ASSIGNED':
        return Colors.green;
      case 'DRIVER_NOT_FOUND':
        return Colors.orange;
      case 'TRIP_STARTED':
        return Colors.blue;
      case 'TRIP_COMPLETED':
        return Colors.green;
      case 'TRIP_REJECTED':
        return Colors.redAccent;
      default:
        return Colors.white54;
    }
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

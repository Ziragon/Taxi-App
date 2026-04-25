import 'package:flutter/material.dart';
import 'package:arbuz_express/widgets/app_ui.dart';

class DriverOnlineToggle extends StatelessWidget {
  final bool isOnline;
  final VoidCallback onToggle;

  const DriverOnlineToggle({
    super.key,
    required this.isOnline,
    required this.onToggle,
  });

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: onToggle,
      child: GlassCard(
        padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 12),
        child: Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            AnimatedContainer(
              duration: const Duration(milliseconds: 200),
              width: 12,
              height: 12,
              decoration: BoxDecoration(
                shape: BoxShape.circle,
                color: isOnline ? Colors.greenAccent : Colors.redAccent,
                boxShadow: [
                  BoxShadow(
                    color: (isOnline ? Colors.greenAccent : Colors.redAccent)
                        .withOpacity(0.5),
                    blurRadius: 8,
                  ),
                ],
              ),
            ),
            const SizedBox(width: 12),
            Text(
              isOnline ? 'Вы на линии' : 'Не активен',
              style: const TextStyle(
                color: Colors.white,
                fontSize: 16,
                fontWeight: FontWeight.w700,
              ),
            ),
            const SizedBox(width: 8),
            Icon(
              isOnline ? Icons.check_circle_outline : Icons.circle_outlined,
              color: Colors.white70,
              size: 18,
            ),
          ],
        ),
      ),
    );
  }
}

import 'package:flutter/material.dart';

class VerificationBanner extends StatelessWidget {
  const VerificationBanner({super.key});

  @override
  Widget build(BuildContext context) {
    return Container(
      margin: const EdgeInsets.symmetric(horizontal: 16),
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
      decoration: BoxDecoration(
        color: const Color(0xFFFFC107).withOpacity(0.15),
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: const Color(0xFFFFC107).withOpacity(0.4)),
      ),
      child: Row(
        children: [
          const Icon(
            Icons.info_outline_rounded,
            color: Color(0xFFFFC107),
            size: 22,
          ),
          const SizedBox(width: 12),
          Expanded(
            child: Text(
              'Ваши данные на проверке. Это займёт 5–10 минут.',
              style: TextStyle(
                color: Colors.white.withOpacity(0.9),
                fontSize: 14,
                fontWeight: FontWeight.w500,
              ),
            ),
          ),
        ],
      ),
    );
  }
}

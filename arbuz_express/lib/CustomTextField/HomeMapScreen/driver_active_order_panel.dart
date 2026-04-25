import 'package:flutter/material.dart';
import 'package:arbuz_express/widgets/app_ui.dart';

class DriverActiveOrderPanel extends StatelessWidget {
  final String clientName;
  final String fromAddress;
  final VoidCallback onArrived;
  final VoidCallback onCancel;

  const DriverActiveOrderPanel({
    super.key,
    required this.clientName,
    required this.fromAddress,
    required this.onArrived,
    required this.onCancel,
  });

  @override
  Widget build(BuildContext context) {
    return GlassCard(
      padding: const EdgeInsets.all(16),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          Row(
            children: [
              Container(
                width: 52,
                height: 52,
                decoration: BoxDecoration(
                  color: const Color(0xFFFFC107).withOpacity(0.2),
                  shape: BoxShape.circle,
                ),
                child: const Icon(Icons.person_rounded, color: Color(0xFFFFC107), size: 28),
              ),
              const SizedBox(width: 16),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    const Text(
                      'Едем к клиенту', 
                      style: TextStyle(color: Colors.white54, fontSize: 12),
                    ),
                    const SizedBox(height: 4),
                    Text(
                      clientName, 
                      style: const TextStyle(
                        color: Colors.white, 
                        fontSize: 18, 
                        fontWeight: FontWeight.bold,
                      ),
                    ),
                  ],
                ),
              ),
              CircleIconButton(
                icon: Icons.phone_rounded,
                onTap: () {
                  // Логика звонка
                },
                color: const Color(0xFF1A1A1E),
              ),
            ],
          ),
          const Divider(color: Colors.white10, height: 24),
          Row(
            children: [
              const Icon(Icons.location_on_rounded, color: Color(0xFFFFC107), size: 20),
              const SizedBox(width: 12),
              Expanded(
                child: Text(
                  fromAddress, 
                  style: const TextStyle(color: Colors.white, fontSize: 14),
                ),
              ),
            ],
          ),
          const SizedBox(height: 20),
          Row(
            children: [
              Expanded(
                child: OutlinedButton(
                  onPressed: onCancel,
                  style: OutlinedButton.styleFrom(
                    foregroundColor: Colors.redAccent,
                    side: BorderSide(color: Colors.redAccent.withOpacity(0.3)),
                    shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(14)),
                    padding: const EdgeInsets.symmetric(vertical: 14),
                  ),
                  child: const Text('Отмена'),
                ),
              ),
              const SizedBox(width: 12),
              Expanded(
                flex: 2,
                child: PrimaryButton(
                  label: 'Я на месте',
                  onPressed: onArrived,
                ),
              ),
            ],
          )
        ],
      ),
    );
  }
}
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:arbuz_express/widgets/app_ui.dart';

class RideHistoryScreen extends StatelessWidget {
  const RideHistoryScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final rides = [
      (
        'Tesla Model 3',
        'A777AA 77',
        'Бизнес',
        'Дмитрий С.',
        'ID: 842910',
        'Dmitry',
      ),
      (
        'Toyota Camry',
        'K123OM 54',
        'Комфорт',
        'Артем П.',
        'ID: 753120',
        'Artem',
      ),
      (
        'Hyundai Solaris',
        'M456HE 154',
        'Эконом',
        'Иван К.',
        'ID: 610234',
        'Ivan',
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
          'История поездок',
          style: TextStyle(
            color: Colors.white,
            fontSize: 18,
            fontWeight: FontWeight.w700,
          ),
        ),
      ),
      body: ListView.builder(
        padding: const EdgeInsets.all(24),
        itemCount: rides.length,
        itemBuilder: (context, index) {
          final ride = rides[index];
          final rideId = ride.$5.replaceAll('ID: ', '');

          return Padding(
            padding: const EdgeInsets.only(bottom: 16),
            child: GestureDetector(
              onLongPress: () {
                Clipboard.setData(ClipboardData(text: rideId));
                HapticFeedback.mediumImpact();
                ScaffoldMessenger.of(context).showSnackBar(
                  SnackBar(
                    content: Text('ID поездки $rideId скопирован'),
                    behavior: SnackBarBehavior.floating,
                    backgroundColor: const Color(0xFF1A1A1E),
                    shape: RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(12),
                    ),
                    duration: const Duration(seconds: 2),
                  ),
                );
              },
              child: GlassCard(
                padding: const EdgeInsets.all(20),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Row(
                      children: [
                        CircleAvatar(
                          radius: 24,
                          backgroundColor: const Color(0xFF1A1A1E),
                          backgroundImage: NetworkImage(
                            'https://api.dicebear.com/7.x/avataaars/png?seed=${ride.$6}',
                          ),
                        ),
                        const SizedBox(width: 16),
                        Expanded(
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Row(
                                mainAxisAlignment:
                                    MainAxisAlignment.spaceBetween,
                                children: [
                                  Text(
                                    ride.$1,
                                    style: const TextStyle(
                                      color: Colors.white,
                                      fontSize: 18,
                                      fontWeight: FontWeight.bold,
                                    ),
                                  ),
                                  Container(
                                    padding: const EdgeInsets.symmetric(
                                      horizontal: 10,
                                      vertical: 4,
                                    ),
                                    decoration: BoxDecoration(
                                      color: const Color(
                                        0xFFFFC107,
                                      ).withOpacity(0.1),
                                      borderRadius: BorderRadius.circular(8),
                                    ),
                                    child: Text(
                                      ride.$3,
                                      style: const TextStyle(
                                        color: Color(0xFFFFC107),
                                        fontSize: 12,
                                        fontWeight: FontWeight.bold,
                                      ),
                                    ),
                                  ),
                                ],
                              ),
                              const SizedBox(height: 4),
                              Text(
                                ride.$2,
                                style: TextStyle(
                                  color: Colors.white.withOpacity(0.6),
                                  fontSize: 14,
                                ),
                              ),
                            ],
                          ),
                        ),
                      ],
                    ),
                    const SizedBox(height: 20),
                    Row(
                      children: [
                        const Icon(
                          Icons.person_outline,
                          color: Colors.white38,
                          size: 16,
                        ),
                        const SizedBox(width: 8),
                        Text(
                          ride.$4,
                          style: const TextStyle(
                            color: Colors.white70,
                            fontSize: 15,
                          ),
                        ),
                        const Spacer(),
                        Text(
                          ride.$5,
                          style: const TextStyle(
                            color: Colors.white24,
                            fontSize: 12,
                          ),
                        ),
                      ],
                    ),
                  ],
                ),
              ),
            ),
          );
        },
      ),
    );
  }
}

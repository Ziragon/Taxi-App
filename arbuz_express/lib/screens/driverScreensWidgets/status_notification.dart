import 'package:flutter/material.dart';
import 'dart:ui';

class StatusNotification extends StatefulWidget {
  final String message;
  final String code;
  final VoidCallback? onDismiss;

  const StatusNotification({
    super.key,
    required this.message,
    required this.code,
    this.onDismiss,
  });

  @override
  State<StatusNotification> createState() => _StatusNotificationState();
}

class _StatusNotificationState extends State<StatusNotification>
    with SingleTickerProviderStateMixin {
  late AnimationController _controller;
  late Animation<double> _scaleAnimation;
  late Animation<double> _fadeAnimation;

  @override
  void initState() {
    super.initState();
    _controller = AnimationController(
      duration: const Duration(milliseconds: 400),
      vsync: this,
    );

    _scaleAnimation = Tween<double>(
      begin: 0.85,
      end: 1.0,
    ).animate(CurvedAnimation(parent: _controller, curve: Curves.easeOutBack));

    _fadeAnimation = Tween<double>(
      begin: 0.0,
      end: 1.0,
    ).animate(CurvedAnimation(parent: _controller, curve: Curves.easeIn));

    _controller.forward();

    Future.delayed(const Duration(seconds: 4), () {
      if (mounted) {
        _handleDismiss();
      }
    });
  }

  void _handleDismiss() {
    _controller.reverse().then((_) {
      if (mounted && Navigator.of(context).canPop()) {
        Navigator.of(context).pop();
        widget.onDismiss?.call();
      }
    });
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  Color _getPrimaryColor() {
    switch (widget.code) {
      case 'NETWORK':
        return const Color(0xFFEF4444);
      case 'ERROR':
        return const Color(0xFFFF9800);
      default:
        return const Color(0xFFFFC107);
    }
  }

  IconData _getIcon() {
    switch (widget.code) {
      case 'ACCOUNT_NOT_VERIFIED':
        return Icons.face_retouching_off_rounded;
      case 'VEHICLE_NOT_VERIFIED':
        return Icons.time_to_leave_rounded;
      case 'VEHICLE_MISSING':
        return Icons.no_crash_rounded;
      case 'NETWORK':
        return Icons.signal_cellular_connected_no_internet_4_bar_rounded;
      case 'ERROR':
        return Icons.running_with_errors_rounded;
      default:
        return Icons.close_rounded;
    }
  }

  String _getTitle() {
    switch (widget.code) {
      case 'ACCOUNT_NOT_VERIFIED':
        return 'Профиль';
      case 'VEHICLE_NOT_VERIFIED':
        return 'Автомобиль';
      case 'VEHICLE_MISSING':
        return 'Подтвердите транспорт';
      case 'NETWORK':
        return 'Соединение';
      case 'ERROR':
        return 'Упс!';
      default:
        return 'Профиль не подтверждён';
    }
  }

  @override
  Widget build(BuildContext context) {
    final accentColor = _getPrimaryColor();

    return FadeTransition(
      opacity: _fadeAnimation,
      child: ScaleTransition(
        scale: _scaleAnimation,
        child: Container(
          margin: const EdgeInsets.symmetric(horizontal: 16, vertical: 20),
          child: ClipRRect(
            borderRadius: BorderRadius.circular(24),
            child: BackdropFilter(
              filter: ImageFilter.blur(sigmaX: 15, sigmaY: 15),
              child: Container(
                padding: const EdgeInsets.all(20),
                decoration: BoxDecoration(
                  color: const Color(0xFF1A1A1E).withOpacity(0.9),
                  borderRadius: BorderRadius.circular(24),
                  border: Border.all(
                    color: accentColor.withOpacity(0.15),
                    width: 1.0,
                  ),
                  boxShadow: [
                    BoxShadow(
                      color: Colors.black.withOpacity(0.2),
                      blurRadius: 20,
                      offset: const Offset(0, 10),
                    ),
                  ],
                ),
                child: IntrinsicHeight(
                  child: Row(
                    children: [
                      Container(
                        padding: const EdgeInsets.all(14),
                        decoration: BoxDecoration(
                          color: accentColor.withOpacity(0.1),
                          borderRadius: BorderRadius.circular(18),
                        ),
                        child: Icon(_getIcon(), color: accentColor, size: 26),
                      ),
                      const SizedBox(width: 16),
                      VerticalDivider(
                        color: Colors.white.withOpacity(0.08),
                        width: 1,
                        thickness: 1,
                      ),
                      const SizedBox(width: 16),
                      Expanded(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text(
                              _getTitle().toUpperCase(),
                              style: TextStyle(
                                color: accentColor,
                                fontSize: 11,
                                fontWeight: FontWeight.w900,
                                letterSpacing: 1.2,
                              ),
                            ),
                            const SizedBox(height: 4),
                            Text(
                              widget.message,
                              style: const TextStyle(
                                color: Colors.white,
                                fontSize: 14,
                                fontWeight: FontWeight.w600,
                                height: 1.3,
                              ),
                            ),
                          ],
                        ),
                      ),
                      const SizedBox(width: 10),
                      GestureDetector(
                        onTap: _handleDismiss,
                        behavior: HitTestBehavior.opaque,
                        child: Container(
                          padding: const EdgeInsets.all(4),
                          child: Icon(
                            Icons.close_rounded,
                            color: Colors.white.withOpacity(0.3),
                            size: 20,
                          ),
                        ),
                      ),
                    ],
                  ),
                ),
              ),
            ),
          ),
        ),
      ),
    );
  }
}

void showStatusNotification(
  BuildContext context, {
  required String message,
  required String code,
  VoidCallback? onDismiss,
}) {
  showGeneralDialog(
    context: context,
    barrierDismissible: true,
    barrierLabel: '',
    barrierColor: Colors.transparent,
    transitionDuration: const Duration(milliseconds: 250),
    pageBuilder: (context, anim1, anim2) {
      return Align(
        alignment: Alignment.bottomCenter,
        child: Material(
          color: Colors.transparent,
          child: StatusNotification(
            message: message,
            code: code,
            onDismiss: onDismiss,
          ),
        ),
      );
    },
  );
}

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:arbuz_express/widgets/app_ui.dart';

class PaymentMethodsScreen extends StatefulWidget {
  const PaymentMethodsScreen({super.key});

  @override
  State<PaymentMethodsScreen> createState() => _PaymentMethodsScreenState();
}

class _PaymentMethodsScreenState extends State<PaymentMethodsScreen> {
  List<String> cards = ['**** **** **** 4412', '**** **** **** 8890'];

  void _showTopNotification(String message, {bool isError = false}) {
    OverlayState? overlayState = Overlay.of(context);
    late OverlayEntry overlayEntry;

    overlayEntry = OverlayEntry(
      builder: (context) => Positioned(
        top: MediaQuery.of(context).padding.top + 10,
        left: 16,
        right: 16,
        child: Material(
          color: Colors.transparent,
          child: Container(
            padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 16),
            decoration: BoxDecoration(
              color: isError
                  ? const Color(0xFFFF5722)
                  : const Color(0xFF1A1A1E),
              borderRadius: BorderRadius.circular(16),
              border: Border.all(color: Colors.white.withOpacity(0.1)),
              boxShadow: [
                BoxShadow(
                  color: Colors.black.withOpacity(0.4),
                  blurRadius: 20,
                  offset: const Offset(0, 10),
                ),
              ],
            ),
            child: Row(
              children: [
                Icon(
                  isError ? Icons.error_outline : Icons.check_circle_outline,
                  color: isError ? Colors.white : const Color(0xFFFFC107),
                ),
                const SizedBox(width: 12),
                Expanded(
                  child: Text(
                    message,
                    style: const TextStyle(
                      color: Colors.white,
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
    );

    overlayState.insert(overlayEntry);
    Future.delayed(const Duration(seconds: 3), () => overlayEntry.remove());
  }

  void _showAddCardSheet() {
    final numberController = TextEditingController();
    final dateController = TextEditingController();
    final cvvController = TextEditingController();

    showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.transparent,
      barrierColor: Colors.black.withOpacity(0.7),
      builder: (context) => StatefulBuilder(
        builder: (context, setModalState) {
          bool isFormValid() {
            return numberController.text.length == 19 &&
                dateController.text.length == 5 &&
                cvvController.text.length == 3;
          }

          void updateState() => setModalState(() {});

          return Container(
            padding: EdgeInsets.only(
              bottom: MediaQuery.of(context).viewInsets.bottom,
            ),
            child: GlassCard(
              radius: 32,
              padding: const EdgeInsets.fromLTRB(32, 12, 32, 32),
              child: Column(
                mainAxisSize: MainAxisSize.min,
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Center(
                    child: Container(
                      width: 40,
                      height: 4,
                      margin: const EdgeInsets.only(bottom: 24),
                      decoration: BoxDecoration(
                        color: Colors.white.withOpacity(0.1),
                        borderRadius: BorderRadius.circular(2),
                      ),
                    ),
                  ),
                  const Text(
                    'Новая карта',
                    style: TextStyle(
                      color: Colors.white,
                      fontSize: 24,
                      fontWeight: FontWeight.w900,
                    ),
                  ),
                  const SizedBox(height: 24),
                  _buildField(
                    'Номер карты',
                    '0000 0000 0000 0000',
                    Icons.credit_card,
                    numberController,
                    TextInputType.number,
                    19,
                    [
                      FilteringTextInputFormatter.digitsOnly,
                      CardNumberFormatter(),
                    ],
                    onChanged: (_) => updateState(),
                  ),
                  const SizedBox(height: 16),
                  Row(
                    children: [
                      Expanded(
                        child: _buildField(
                          'Срок действия',
                          'ММ/ГГ',
                          Icons.calendar_today,
                          dateController,
                          TextInputType.number,
                          5,
                          [
                            FilteringTextInputFormatter.digitsOnly,
                            CardDateFormatter(),
                          ],
                          onChanged: (_) => updateState(),
                        ),
                      ),
                      const SizedBox(width: 16),
                      Expanded(
                        child: _buildField(
                          'CVV',
                          '123',
                          Icons.lock_outline,
                          cvvController,
                          TextInputType.number,
                          3,
                          [FilteringTextInputFormatter.digitsOnly],
                          onChanged: (_) => updateState(),
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 32),
                  Opacity(
                    opacity: isFormValid() ? 1.0 : 0.5,
                    child: PrimaryButton(
                      label: 'Привязать карту',
                      onPressed: isFormValid()
                          ? () {
                              final lastFour = numberController.text.substring(
                                numberController.text.length - 4,
                              );
                              Navigator.pop(context);
                              setState(() {
                                cards.add('**** **** **** $lastFour');
                              });
                              _showTopNotification(
                                'Карта **** $lastFour успешно добавлена',
                              );
                            }
                          : () {},
                    ),
                  ),
                ],
              ),
            ),
          );
        },
      ),
    );
  }

  Widget _buildField(
    String label,
    String hint,
    IconData icon,
    TextEditingController controller,
    TextInputType type,
    int maxLength,
    List<TextInputFormatter>? formatters, {
    required Function(String) onChanged,
  }) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          label,
          style: const TextStyle(
            color: Colors.white70,
            fontSize: 13,
            fontWeight: FontWeight.w600,
          ),
        ),
        const SizedBox(height: 8),
        Container(
          padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 4),
          decoration: BoxDecoration(
            color: Colors.white.withOpacity(0.05),
            borderRadius: BorderRadius.circular(16),
            border: Border.all(color: Colors.white.withOpacity(0.05)),
          ),
          child: TextField(
            controller: controller,
            keyboardType: type,
            inputFormatters: formatters,
            maxLength: maxLength,
            onChanged: onChanged,
            style: const TextStyle(color: Colors.white, letterSpacing: 1.5),
            decoration: InputDecoration(
              counterText: '',
              icon: Icon(icon, color: const Color(0xFFFFC107), size: 20),
              border: InputBorder.none,
              hintText: hint,
              hintStyle: TextStyle(
                color: Colors.white.withOpacity(0.15),
                letterSpacing: 1.5,
              ),
            ),
          ),
        ),
      ],
    );
  }

  @override
  Widget build(BuildContext context) {
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
          'Способы оплаты',
          style: TextStyle(
            color: Colors.white,
            fontSize: 18,
            fontWeight: FontWeight.w700,
          ),
        ),
      ),
      body: Padding(
        padding: const EdgeInsets.all(24),
        child: Column(
          children: [
            Expanded(
              child: cards.isEmpty
                  ? Center(
                      child: Column(
                        mainAxisAlignment: MainAxisAlignment.center,
                        children: [
                          Icon(
                            Icons.sentiment_very_dissatisfied,
                            size: 80,
                            color: Colors.white.withOpacity(0.1),
                          ),
                          const SizedBox(height: 24),
                          const Text(
                            'Пусто...',
                            style: TextStyle(
                              color: Colors.white,
                              fontSize: 20,
                              fontWeight: FontWeight.bold,
                            ),
                          ),
                          const SizedBox(height: 8),
                          Text(
                            'Если нечем платить, возьмём в рабство',
                            textAlign: TextAlign.center,
                            style: TextStyle(
                              color: Colors.white.withOpacity(0.4),
                              fontSize: 15,
                            ),
                          ),
                        ],
                      ),
                    )
                  : ListView.builder(
                      itemCount: cards.length,
                      itemBuilder: (context, index) {
                        return Padding(
                          padding: const EdgeInsets.only(bottom: 12),
                          child: GlassCard(
                            padding: const EdgeInsets.symmetric(
                              horizontal: 20,
                              vertical: 16,
                            ),
                            child: Row(
                              children: [
                                const Icon(
                                  Icons.credit_card,
                                  color: Color(0xFFFFC107),
                                ),
                                const SizedBox(width: 16),
                                Text(
                                  cards[index],
                                  style: const TextStyle(
                                    color: Colors.white,
                                    fontSize: 16,
                                  ),
                                ),
                                const Spacer(),
                                IconButton(
                                  icon: const Icon(
                                    Icons.delete_outline,
                                    color: Color(0xFFFF5722),
                                  ),
                                  onPressed: () {
                                    final cardName = cards[index];
                                    setState(() => cards.removeAt(index));
                                    _showTopNotification(
                                      'Карта $cardName удалена',
                                      isError: true,
                                    );
                                  },
                                ),
                              ],
                            ),
                          ),
                        );
                      },
                    ),
            ),
            PrimaryButton(
              label: 'Добавить карту',
              onPressed: _showAddCardSheet,
            ),
          ],
        ),
      ),
    );
  }
}

class CardNumberFormatter extends TextInputFormatter {
  @override
  TextEditingValue formatEditUpdate(
    TextEditingValue oldValue,
    TextEditingValue newValue,
  ) {
    String text = newValue.text.replaceAll(' ', '');
    String formatted = "";
    for (int i = 0; i < text.length; i++) {
      formatted += text[i];
      if ((i + 1) % 4 == 0 && (i + 1) != text.length) {
        formatted += " ";
      }
    }
    return TextEditingValue(
      text: formatted,
      selection: TextSelection.collapsed(offset: formatted.length),
    );
  }
}

class CardDateFormatter extends TextInputFormatter {
  @override
  TextEditingValue formatEditUpdate(
    TextEditingValue oldValue,
    TextEditingValue newValue,
  ) {
    String text = newValue.text.replaceAll('/', '');
    String formatted = "";
    for (int i = 0; i < text.length; i++) {
      formatted += text[i];
      if (i == 1 && text.length > 2) {
        formatted += "/";
      }
    }
    return TextEditingValue(
      text: formatted,
      selection: TextSelection.collapsed(offset: formatted.length),
    );
  }
}

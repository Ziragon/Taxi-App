import 'package:flutter/material.dart';
import 'package:flutter_stripe/flutter_stripe.dart';
import 'package:arbuz_express/hooks/use_payment.dart';
import 'package:arbuz_express/widgets/app_ui.dart';
import 'package:arbuz_express/services/token_storage.dart';

class PaymentMethodsScreen extends StatefulWidget {
  const PaymentMethodsScreen({super.key});

  @override
  State<PaymentMethodsScreen> createState() => _PaymentMethodsScreenState();
}

class _PaymentMethodsScreenState extends State<PaymentMethodsScreen> {
  final _usePayment = UsePayment();
  List<Map<String, dynamic>> _cards = [];
  bool _isLoading = true;
  int? _settingDefaultId;

  bool get _isDriver => TokenStorage.userRole == 'driver';

  @override
  void initState() {
    super.initState();
    _loadCards();
  }

  Future<void> _loadCards() async {
    if (!mounted) return;
    setState(() => _isLoading = true);
    final cards = await _usePayment.getPaymentMethods();
    if (!mounted) return;
    setState(() {
      _cards = cards;
      _isLoading = false;
    });
  }

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
    CardFieldInputDetails? cardDetails;
    final ValueNotifier<bool> isValidNotifier = ValueNotifier(false);
    final ValueNotifier<bool> isSubmittingNotifier = ValueNotifier(false);

    showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.transparent,
      barrierColor: Colors.black.withOpacity(0.7),
      builder: (context) => Padding(
        padding: EdgeInsets.only(
          bottom: MediaQuery.of(context).viewInsets.bottom,
        ),
        child: SingleChildScrollView(
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
                Text(
                  _isDriver ? 'Новый счёт' : 'Новая карта',
                  style: const TextStyle(
                    color: Colors.white,
                    fontSize: 24,
                    fontWeight: FontWeight.w900,
                  ),
                ),
                const SizedBox(height: 24),
                Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Padding(
                      padding: const EdgeInsets.only(left: 4, bottom: 8),
                      child: Text(
                        'ДАННЫЕ КАРТЫ',
                        style: TextStyle(
                          color: Colors.white.withOpacity(0.4),
                          fontSize: 10,
                          fontWeight: FontWeight.w900,
                          letterSpacing: 1.5,
                        ),
                      ),
                    ),
                    Container(
                      padding: const EdgeInsets.symmetric(
                        horizontal: 16,
                        vertical: 14,
                      ),
                      decoration: BoxDecoration(
                        color: Colors.white.withOpacity(0.04),
                        borderRadius: BorderRadius.circular(16),
                        border: Border.all(
                          color: Colors.white.withOpacity(0.06),
                        ),
                      ),
                      child: CardField(
                        enablePostalCode: false,
                        autofocus: true,
                        cursorColor: const Color(0xFFFFC107),
                        style: const TextStyle(
                          color: Colors.white,
                          fontSize: 16,
                          height: 1.4,
                        ),
                        decoration: const InputDecoration(
                          border: InputBorder.none,
                          isDense: true,
                          contentPadding: EdgeInsets.zero,
                        ),
                        numberHintText: '•••• •••• •••• ••••',
                        expirationHintText: 'MM/YY',
                        cvcHintText: 'CVC',
                        onCardChanged: (details) {
                          cardDetails = details;
                          isValidNotifier.value = details?.complete ?? false;
                        },
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 32),
                ValueListenableBuilder<bool>(
                  valueListenable: isValidNotifier,
                  builder: (context, isValid, child) {
                    return ValueListenableBuilder<bool>(
                      valueListenable: isSubmittingNotifier,
                      builder: (context, isSubmitting, child) {
                        final bool canSubmit = isValid && !isSubmitting;
                        return Opacity(
                          opacity: canSubmit ? 1.0 : 0.5,
                          child: PrimaryButton(
                            label: _isDriver
                                ? 'Привязать счёт'
                                : 'Привязать карту',
                            onPressed: canSubmit
                                ? () async {
                                    isSubmittingNotifier.value = true;
                                    try {
                                      final credentialId = await _usePayment
                                          .createStripeCredential(
                                            cardholderName: 'User',
                                            postalCode: cardDetails?.postalCode,
                                          );

                                      if (credentialId != null) {
                                        final lastFour =
                                            cardDetails?.last4 ?? '';

                                        await _usePayment
                                            .addPaymentMethodToBackend(
                                              credentialId,
                                              lastFour,
                                            );
                                        if (mounted) {
                                          Navigator.pop(context);
                                          _loadCards();
                                          _showTopNotification(
                                            _isDriver
                                                ? 'Счёт привязан'
                                                : 'Карта привязана',
                                          );
                                        }
                                      } else {
                                        isSubmittingNotifier.value = false;
                                      }
                                    } catch (e) {
                                      if (mounted) {
                                        isSubmittingNotifier.value = false;
                                        Navigator.pop(context);
                                        _showTopNotification(
                                          e.toString(),
                                          isError: true,
                                        );
                                      }
                                    }
                                  }
                                : () {},
                          ),
                        );
                      },
                    );
                  },
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }

  String _formatCardLabel(Map<String, dynamic> card) {
    final last4 = (card['lastFour'] as String?) ?? '';
    final brand = (card['cardBrand'] as String?) ?? '';
    if (_isDriver) {
      return 'Счёт •••• $last4';
    }
    return '${brand.toUpperCase()} •••• $last4';
  }

  Future<void> _deleteCard(Map<String, dynamic> card) async {
    final id = card['id'] as int;
    final success = await _usePayment.deletePaymentMethod(id);
    if (success) {
      await _loadCards();
      _showTopNotification('Удалено', isError: true);
    }
  }

  Future<void> _setDefault(Map<String, dynamic> card) async {
    final id = card['id'] as int;
    setState(() => _settingDefaultId = id);
    final success = await _usePayment.setDefaultPaymentMethod(id);
    if (success) {
      await _loadCards();
      _showTopNotification('Обновлено');
    }
    setState(() => _settingDefaultId = null);
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
        title: Text(
          _isDriver ? 'Счета для выплат' : 'Способы оплаты',
          style: const TextStyle(
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
              child: _isLoading
                  ? const Center(
                      child: CircularProgressIndicator(
                        color: Color(0xFFFFC107),
                      ),
                    )
                  : _cards.isEmpty
                  ? Center(
                      child: Column(
                        mainAxisAlignment: MainAxisAlignment.center,
                        children: [
                          Icon(
                            _isDriver
                                ? Icons.account_balance
                                : Icons.credit_card_off,
                            size: 64,
                            color: Colors.white10,
                          ),
                          const SizedBox(height: 16),
                          const Text(
                            'Список пуст',
                            style: TextStyle(color: Colors.white),
                          ),
                        ],
                      ),
                    )
                  : ListView.builder(
                      itemCount: _cards.length,
                      itemBuilder: (context, index) {
                        final card = _cards[index];
                        final isDefault = card['isDefault'] == true;
                        final id = card['id'] as int;
                        return Padding(
                          padding: const EdgeInsets.only(bottom: 12),
                          child: GlassCard(
                            padding: const EdgeInsets.all(16),
                            child: Row(
                              children: [
                                Icon(
                                  _isDriver
                                      ? Icons.account_balance
                                      : Icons.credit_card,
                                  color: isDefault
                                      ? const Color(0xFFFFC107)
                                      : Colors.white38,
                                ),
                                const SizedBox(width: 16),
                                Expanded(
                                  child: Column(
                                    crossAxisAlignment:
                                        CrossAxisAlignment.start,
                                    children: [
                                      Text(
                                        _formatCardLabel(card),
                                        style: const TextStyle(
                                          color: Colors.white,
                                        ),
                                      ),
                                      if (isDefault) ...[
                                        const SizedBox(height: 4),
                                        Text(
                                          _isDriver ? 'Основной' : 'Основная',
                                          style: const TextStyle(
                                            color: Color(0xFFFFC107),
                                            fontSize: 10,
                                          ),
                                        ),
                                      ],
                                    ],
                                  ),
                                ),
                                if (!isDefault)
                                  _settingDefaultId == id
                                      ? const SizedBox(
                                          width: 24,
                                          height: 24,
                                          child: CircularProgressIndicator(
                                            strokeWidth: 2,
                                          ),
                                        )
                                      : IconButton(
                                          icon: const Icon(
                                            Icons.star_border,
                                            color: Colors.white54,
                                          ),
                                          onPressed: () => _setDefault(card),
                                        ),
                                IconButton(
                                  icon: const Icon(
                                    Icons.delete_outline,
                                    color: Colors.redAccent,
                                  ),
                                  onPressed: () => _deleteCard(card),
                                ),
                              ],
                            ),
                          ),
                        );
                      },
                    ),
            ),
            PrimaryButton(
              label: _isDriver ? 'Добавить счёт' : 'Добавить карту',
              onPressed: _showAddCardSheet,
            ),
          ],
        ),
      ),
    );
  }
}

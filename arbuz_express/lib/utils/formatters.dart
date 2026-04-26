import 'package:flutter/services.dart';

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

class ExpiryDateFormatter extends TextInputFormatter {
  @override
  TextEditingValue formatEditUpdate(
    TextEditingValue oldValue,
    TextEditingValue newValue,
  ) {
    String text = newValue.text.replaceAll('/', '');
    if (text.length > 4) text = text.substring(0, 4);
    String formatted = text;
    if (text.length >= 2) {
      formatted =
          text.substring(0, 2) +
          (text.length > 2 ? '/' + text.substring(2) : '');
    }
    return TextEditingValue(
      text: formatted,
      selection: TextSelection.collapsed(offset: formatted.length),
    );
  }
}

class Validators {
  static bool validateEmail(String email) {
    return RegExp(r'^[\w-\.]+@([\w-]+\.)+[\w-]{2,4}$').hasMatch(email);
  }

  static bool validatePhone(String phone) {
    final phoneClean = phone.replaceAll(RegExp(r'[^0-9]'), '');
    return phoneClean.length == 11;
  }

  static bool validatePassword(String password) {
    return password.length >= 6 &&
        RegExp(r'[A-Z]').hasMatch(password) &&
        RegExp(r'\d').hasMatch(password) &&
        RegExp(r'[!@#$%^&*(),.?":{}|<>]').hasMatch(password);
  }

  static bool validateFio(String fio) {
    final fioWords = fio.split(' ').where((w) => w.isNotEmpty).toList();
    return fioWords.length >= 2;
  }

  static bool validateCard(String card) {
    final cardClean = card.replaceAll(' ', '');
    return cardClean.length == 16;
  }

  static bool validateCvv(String cvv) {
    return cvv.length == 3 && int.tryParse(cvv) != null;
  }

  static bool validateExpiry(String expiry) {
    if (expiry.length != 5 || !expiry.contains('/')) return false;

    final parts = expiry.split('/');
    if (parts.length != 2) return false;

    final month = int.tryParse(parts[0]);
    final year = int.tryParse(parts[1]);

    if (month == null || year == null || month < 1 || month > 12) {
      return false;
    }

    final now = DateTime.now();
    final currentYear = now.year % 100;
    final currentMonth = now.month;

    return year > currentYear || (year == currentYear && month >= currentMonth);
  }

  static bool validateLicense(String license) {
    return RegExp(r'^\d{10}$').hasMatch(license);
  }

  static bool validatePlate(String plate) {
    return RegExp(r'^[АВЕКМНОРСТУХ]\d{3}[АВЕКМНОРСТУХ]{2}\d{2,3}$')
        .hasMatch(plate);
  }

  static bool validateYear(String yearText) {
    if (yearText.isEmpty) return false;
    final year = int.tryParse(yearText);
    final currentYear = DateTime.now().year;
    return year != null && year >= 1900 && year <= currentYear;
  }
}
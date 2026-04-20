import 'package:arbuz_express/main.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  testWidgets('shows auth screen and opens map flow', (tester) async {
    await tester.pumpWidget(const ArbuzExpressApp());

    expect(find.text('Вход по номеру'), findsOneWidget);
    expect(find.text('Получить SMS-код'), findsOneWidget);

    await tester.tap(find.text('Получить SMS-код'));
    await tester.pumpAndSettle();

    expect(find.text('Куда едем?'), findsOneWidget);
    expect(find.text('Заказать поездку'), findsOneWidget);
  });
}

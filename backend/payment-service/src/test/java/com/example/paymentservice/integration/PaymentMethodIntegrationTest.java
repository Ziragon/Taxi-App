package com.example.paymentservice.integration;

import com.example.paymentservice.entity.PaymentMethod;
import com.example.paymentservice.exception.DefaultPaymentMethodException;
import com.example.paymentservice.exception.DuplicatePaymentMethodException;
import com.example.paymentservice.exception.PaymentMethodNotActiveException;
import com.example.paymentservice.exception.PaymentMethodNotFoundException;
import com.example.paymentservice.repository.PaymentMethodRepository;
import com.example.paymentservice.service.PaymentMethodService;
import com.example.paymentservice.service.StripeService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("Payment Method Flow Integration Tests")
class PaymentMethodIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private PaymentMethodService paymentMethodService;

    @Autowired
    private PaymentMethodRepository paymentMethodRepository;

    @MockitoBean
    private StripeService stripeService;

    @BeforeEach
    void setUp() {
        paymentMethodRepository.deleteAll();

        StripeService.FakeCustomer mockCustomer = new StripeService.FakeCustomer("cus_test123");

        StripeService.FakePaymentMethod mockStripePaymentMethod =
                new StripeService.FakePaymentMethod("pm_test123", "visa", "4242");

        when(stripeService.getOrCreateCustomer(anyLong())).thenReturn(mockCustomer);
        when(stripeService.attachPaymentMethodToCustomer(anyString(), anyString()))
                .thenReturn(mockStripePaymentMethod);
    }

    @Test
    @DisplayName("Полный flow: добавление карты -> смена default -> деактивация")
    void fullPaymentMethodFlow() {
        PaymentMethod first = paymentMethodService.addPaymentMethod(
                100L, "pm_test123", false
        );

        assertThat(first).satisfies(pm -> {
            assertThat(pm.getPassengerId()).isEqualTo(100L);
            assertThat(pm.isDefaultvalue()).isTrue();
            assertThat(pm.isActive()).isTrue();
            assertThat(pm.getCardBrand()).isEqualTo("visa");
            assertThat(pm.getLastFour()).isEqualTo("4242");
        });

        StripeService.FakePaymentMethod secondStripeMethod =
                new StripeService.FakePaymentMethod("pm_second456", "mastercard", "5555");

        when(stripeService.attachPaymentMethodToCustomer(anyString(), anyString()))
                .thenReturn(secondStripeMethod);

        PaymentMethod second = paymentMethodService.addPaymentMethod(
                100L, "pm_second456", false
        );

        assertThat(second.isDefaultvalue()).isFalse();
        assertThat(second.isActive()).isTrue();

        List<PaymentMethod> active = paymentMethodService.getActiveByPassengerId(100L);
        assertThat(active).hasSize(2);

        paymentMethodService.setDefault(100L, second.getId());

        entityManager.flush();
        entityManager.clear();

        PaymentMethod newDefault = paymentMethodService.getDefaultForPassenger(100L);
        assertThat(newDefault.getId()).isEqualTo(second.getId());
        assertThat(newDefault.isDefaultvalue()).isTrue();

        paymentMethodService.deactivate(100L, first.getId());

        entityManager.flush();
        entityManager.clear();

        List<PaymentMethod> afterDeactivation = paymentMethodService.getActiveByPassengerId(100L);
        assertThat(afterDeactivation).hasSize(1);
        assertThat(afterDeactivation.getFirst().getId()).isEqualTo(second.getId());
    }

    @Test
    @DisplayName("Первая карта всегда становится default независимо от флага setAsDefault")
    void firstCardAlwaysBecomesDefault() {
        PaymentMethod method = paymentMethodService.addPaymentMethod(
                200L, "pm_test123", false
        );

        assertThat(method.isDefaultvalue()).isTrue();
    }

    @Test
    @DisplayName("Вторая карта с флагом setAsDefault=true становится default")
    void secondCardWithFlagBecomesDefault() {
        paymentMethodService.addPaymentMethod(100L, "pm_test123", false);

        StripeService.FakePaymentMethod secondStripe =
                new StripeService.FakePaymentMethod("pm_second", "mastercard", "5555");

        when(stripeService.attachPaymentMethodToCustomer(anyString(), anyString()))
                .thenReturn(secondStripe);

        PaymentMethod second = paymentMethodService.addPaymentMethod(100L, "pm_second", true);

        assertThat(second.isDefaultvalue()).isTrue();

        entityManager.flush();
        entityManager.clear();

        PaymentMethod currentDefault = paymentMethodService.getDefaultForPassenger(100L);
        assertThat(currentDefault.getId()).isEqualTo(second.getId());
    }

    @Test
    @DisplayName("Дублирующая карта выбрасывает DuplicatePaymentMethodException")
    void duplicateCardThrowsException() {
        paymentMethodService.addPaymentMethod(100L, "pm_test123", false);

        assertThatThrownBy(() ->
                paymentMethodService.addPaymentMethod(100L, "pm_test123", false)
        ).isInstanceOf(DuplicatePaymentMethodException.class);
    }

    @Test
    @DisplayName("Нельзя деактивировать default карту")
    void cannotDeactivateDefaultCard() {
        PaymentMethod method = paymentMethodService.addPaymentMethod(100L, "pm_test123", false);

        assertThatThrownBy(() ->
                paymentMethodService.deactivate(100L, method.getId())
        ).isInstanceOf(DefaultPaymentMethodException.class);
    }

    @Test
    @DisplayName("Нельзя установить неактивный метод как default")
    void cannotSetInactiveMethodAsDefault() {
        paymentMethodService.addPaymentMethod(100L, "pm_first", false);

        StripeService.FakePaymentMethod secondStripe =
                new StripeService.FakePaymentMethod("pm_second", "mastercard", "5555");

        when(stripeService.attachPaymentMethodToCustomer(anyString(), anyString()))
                .thenReturn(secondStripe);

        PaymentMethod second = paymentMethodService.addPaymentMethod(100L, "pm_second", false);

        paymentMethodRepository.deactivateById(second.getId());

        entityManager.flush();
        entityManager.clear();

        assertThatThrownBy(() ->
                paymentMethodService.setDefault(100L, second.getId())
        ).isInstanceOf(PaymentMethodNotActiveException.class);
    }

    @Test
    @DisplayName("getById бросает исключение если метод не найден")
    void getByIdThrowsWhenNotFound() {
        assertThatThrownBy(() ->
                paymentMethodService.getById(999L)
        ).isInstanceOf(PaymentMethodNotFoundException.class);
    }

    @Test
    @DisplayName("getDefaultForPassenger бросает исключение если default не найден")
    void getDefaultThrowsWhenNoDefault() {
        assertThatThrownBy(() ->
                paymentMethodService.getDefaultForPassenger(999L)
        ).isInstanceOf(PaymentMethodNotFoundException.class);
    }

    @Test
    @DisplayName("getAllByPassengerId возвращает все карты пассажира")
    void getAllByPassengerIdReturnsAllCards() {
        paymentMethodService.addPaymentMethod(100L, "pm_first", false);

        StripeService.FakePaymentMethod secondStripe =
                new StripeService.FakePaymentMethod("pm_second", "mastercard", "5555");

        when(stripeService.attachPaymentMethodToCustomer(anyString(), anyString()))
                .thenReturn(secondStripe);

        PaymentMethod second = paymentMethodService.addPaymentMethod(100L, "pm_second", false);

        paymentMethodService.setDefault(100L, second.getId());

        entityManager.flush();
        entityManager.clear();

        PaymentMethod first = paymentMethodRepository.findAllByPassengerId(100L).stream()
                .filter(pm -> !pm.isDefaultvalue())
                .findFirst()
                .orElseThrow();

        paymentMethodService.deactivate(100L, first.getId());

        entityManager.flush();
        entityManager.clear();

        List<PaymentMethod> all = paymentMethodService.getAllByPassengerId(100L);
        assertThat(all).hasSize(2);

        List<PaymentMethod> active = paymentMethodService.getActiveByPassengerId(100L);
        assertThat(active).hasSize(1);
    }
}
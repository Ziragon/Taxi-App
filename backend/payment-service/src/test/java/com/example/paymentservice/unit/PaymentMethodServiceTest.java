package com.example.paymentservice.unit;

import com.example.paymentservice.entity.PaymentMethod;
import com.example.paymentservice.exception.DefaultPaymentMethodException;
import com.example.paymentservice.exception.DuplicatePaymentMethodException;
import com.example.paymentservice.exception.PaymentMethodNotActiveException;
import com.example.paymentservice.exception.PaymentMethodNotFoundException;
import com.example.paymentservice.repository.PaymentMethodRepository;
import com.example.paymentservice.service.PaymentMethodService;
import com.example.paymentservice.service.StripeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentMethodService Tests")
class PaymentMethodServiceTest {

    @Mock
    private PaymentMethodRepository repository;

    @Mock
    private StripeService stripeService;

    @InjectMocks
    private PaymentMethodService service;

    private StripeService.FakeCustomer mockCustomer;
    private StripeService.FakePaymentMethod mockStripePaymentMethod;
    private PaymentMethod mockPaymentMethod;

    @BeforeEach
    void setUp() {

        mockCustomer = new StripeService.FakeCustomer("cus_test123");

        mockStripePaymentMethod = new StripeService.FakePaymentMethod("pm_test123", "visa", "4242");

        mockPaymentMethod = PaymentMethod.builder()
                .id(1L)
                .passengerId(100L)
                .stripeCustomerId("cus_test123")
                .stripePaymentMethodId("pm_test123")
                .cardBrand("visa")
                .lastFour("4242")
                .defaultvalue(true)
                .active(true)
                .build();
    }

    @Test
    @DisplayName("Should add first payment method successfully")
    void shouldAddFirstPaymentMethod() {
        Long passengerId = 100L;
        String stripePaymentMethodId = "pm_test123";

        when(repository.existsByPassengerIdAndStripePaymentMethodId(passengerId, stripePaymentMethodId))
                .thenReturn(false);
        when(stripeService.getOrCreateCustomer(passengerId)).thenReturn(mockCustomer);
        when(stripeService.attachPaymentMethodToCustomer(stripePaymentMethodId, mockCustomer.id()))
                .thenReturn(mockStripePaymentMethod);
        when(repository.findAllByPassengerId(passengerId)).thenReturn(Collections.emptyList());
        when(repository.save(any(PaymentMethod.class))).thenReturn(mockPaymentMethod);

        PaymentMethod result = service.addPaymentMethod(passengerId, stripePaymentMethodId, false);

        assertThat(result).isNotNull();
        assertThat(result.getPassengerId()).isEqualTo(passengerId);
        assertThat(result.isDefaultvalue()).isTrue();
        assertThat(result.isActive()).isTrue();

        verify(repository).save(argThat(PaymentMethod::isDefaultvalue));
        verify(stripeService).setDefaultPaymentMethod(mockCustomer.id(), stripePaymentMethodId);
    }

    @Test
    @DisplayName("Should add payment method and set as default when requested")
    void shouldAddPaymentMethodAndSetAsDefault() {
        Long passengerId = 100L;
        String stripePaymentMethodId = "pm_test123";

        PaymentMethod existingMethod = PaymentMethod.builder()
                .id(2L)
                .passengerId(passengerId)
                .defaultvalue(true)
                .build();

        when(repository.existsByPassengerIdAndStripePaymentMethodId(passengerId, stripePaymentMethodId))
                .thenReturn(false);
        when(stripeService.getOrCreateCustomer(passengerId)).thenReturn(mockCustomer);
        when(stripeService.attachPaymentMethodToCustomer(stripePaymentMethodId, mockCustomer.id()))
                .thenReturn(mockStripePaymentMethod);
        when(repository.findAllByPassengerId(passengerId)).thenReturn(List.of(existingMethod));
        when(repository.save(any(PaymentMethod.class))).thenReturn(mockPaymentMethod);

        PaymentMethod result = service.addPaymentMethod(passengerId, stripePaymentMethodId, true);

        assertThat(result).isNotNull();
        assertThat(result.isDefaultvalue()).isTrue();

        verify(repository).clearDefaultForPassenger(passengerId);
        verify(stripeService).setDefaultPaymentMethod(mockCustomer.id(), stripePaymentMethodId);
    }

    @Test
    @DisplayName("Should throw exception when duplicate payment method")
    void shouldThrowExceptionWhenDuplicate() {
        Long passengerId = 100L;
        String stripePaymentMethodId = "pm_test123";

        when(repository.existsByPassengerIdAndStripePaymentMethodId(passengerId, stripePaymentMethodId))
                .thenReturn(true);

        assertThatThrownBy(() -> service.addPaymentMethod(passengerId, stripePaymentMethodId, false))
                .isInstanceOf(DuplicatePaymentMethodException.class)
                .hasMessageContaining("already exists");

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Should get payment method by ID")
    void shouldGetById() {
        when(repository.findById(1L)).thenReturn(Optional.of(mockPaymentMethod));

        PaymentMethod result = service.getById(1L);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Should throw exception when payment method not found")
    void shouldThrowExceptionWhenNotFound() {
        when(repository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(999L))
                .isInstanceOf(PaymentMethodNotFoundException.class);
    }

    @Test
    @DisplayName("Should get default payment method for passenger")
    void shouldGetDefaultForPassenger() {
        Long passengerId = 100L;
        when(repository.findByPassengerIdAndDefaultvalueTrue(passengerId))
                .thenReturn(Optional.of(mockPaymentMethod));

        PaymentMethod result = service.getDefaultForPassenger(passengerId);

        assertThat(result).isNotNull();
        assertThat(result.isDefaultvalue()).isTrue();
    }

    @Test
    @DisplayName("Should get active payment methods for passenger")
    void shouldGetActiveByPassengerId() {
        Long passengerId = 100L;
        List<PaymentMethod> methods = List.of(mockPaymentMethod);
        when(repository.findAllByPassengerIdAndActiveTrue(passengerId)).thenReturn(methods);

        List<PaymentMethod> result = service.getActiveByPassengerId(passengerId);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().isActive()).isTrue();
    }

    @Test
    @DisplayName("Should set default payment method")
    void shouldSetDefault() {
        Long passengerId = 100L;
        Long paymentMethodId = 1L;
        mockPaymentMethod.setDefaultvalue(false);
        when(repository.findById(paymentMethodId)).thenReturn(Optional.of(mockPaymentMethod));
        when(repository.save(any())).thenReturn(mockPaymentMethod);

        service.setDefault(passengerId, paymentMethodId);

        verify(repository).clearDefaultForPassenger(passengerId);
        verify(repository).save(argThat(PaymentMethod::isDefaultvalue));
        verify(stripeService).setDefaultPaymentMethod(
                mockPaymentMethod.getStripeCustomerId(),
                mockPaymentMethod.getStripePaymentMethodId()
        );
    }

    @Test
    @DisplayName("Should throw exception when setting inactive payment method as default")
    void shouldThrowExceptionWhenSetInactiveAsDefault() {
        Long passengerId = 100L;
        Long paymentMethodId = 1L;
        mockPaymentMethod.setActive(false);
        when(repository.findById(paymentMethodId)).thenReturn(Optional.of(mockPaymentMethod));

        assertThatThrownBy(() -> service.setDefault(passengerId, paymentMethodId))
                .isInstanceOf(PaymentMethodNotActiveException.class);
    }

    @Test
    @DisplayName("Should deactivate payment method")
    void shouldDeactivate() {
        Long passengerId = 100L;
        Long paymentMethodId = 1L;
        mockPaymentMethod.setDefaultvalue(false);
        when(repository.findById(paymentMethodId)).thenReturn(Optional.of(mockPaymentMethod));

        service.deactivate(passengerId, paymentMethodId);

        verify(stripeService).detachPaymentMethod(mockPaymentMethod.getStripePaymentMethodId());
        verify(repository).deleteById(paymentMethodId);
    }

    @Test
    @DisplayName("Should throw exception when deactivating default payment method")
    void shouldThrowExceptionWhenDeactivateDefault() {
        Long passengerId = 100L;
        Long paymentMethodId = 1L;
        PaymentMethod otherCard = PaymentMethod.builder()
                .id(2L)
                .passengerId(passengerId)
                .active(true)
                .defaultvalue(false)
                .build();

        when(repository.findById(paymentMethodId)).thenReturn(Optional.of(mockPaymentMethod));
        when(repository.findAllByPassengerIdAndActiveTrue(passengerId))
                .thenReturn(List.of(otherCard));

        assertThatThrownBy(() -> service.deactivate(passengerId, paymentMethodId))
                .isInstanceOf(DefaultPaymentMethodException.class);

        verify(repository, never()).deleteById(anyLong());
    }
}
package com.example.paymentservice.unit;

import com.example.paymentservice.entity.DriverPayoutAccount;
import com.example.paymentservice.entity.PaymentMethod;
import com.example.paymentservice.entity.Transaction;
import com.example.paymentservice.entity.enums.TransactionStatus;
import com.example.paymentservice.entity.enums.TransactionType;
import com.example.paymentservice.exception.InvalidPaymentOperationException;
import com.example.paymentservice.exception.PaymentProcessingException;
import com.example.paymentservice.exception.TransactionNotFoundException;
import com.example.paymentservice.repository.TransactionRepository;
import com.example.paymentservice.service.DriverPayoutAccountService;
import com.example.paymentservice.service.PaymentMethodService;
import com.example.paymentservice.service.StripeService;
import com.example.paymentservice.service.TransactionService;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Refund;
import com.stripe.model.Transfer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TransactionService Tests")
class TransactionServiceTest {

    @Mock
    private TransactionRepository repository;

    @Mock
    private PaymentMethodService paymentMethodService;

    @Mock
    private DriverPayoutAccountService driverPayoutAccountService;

    @Mock
    private StripeService stripeService;

    @InjectMocks
    private TransactionService service;

    private PaymentMethod mockPaymentMethod;
    private DriverPayoutAccount mockPayoutAccount;
    private PaymentIntent mockPaymentIntent;
    private Transaction mockTransaction;

    @BeforeEach
    void setUp() {
        mockPaymentMethod = PaymentMethod.builder()
                .id(1L)
                .passengerId(100L)
                .stripeCustomerId("cus_test123")
                .stripePaymentMethodId("pm_test123")
                .active(true)
                .build();

        mockPayoutAccount = DriverPayoutAccount.builder()
                .id(1L)
                .driverId(200L)
                .stripeAccountId("acct_test123")
                .verified(true)
                .defaultvalue(true)
                .build();

        mockPaymentIntent = new PaymentIntent();
        mockPaymentIntent.setId("pi_test123");
        mockPaymentIntent.setStatus("succeeded");

        mockTransaction = Transaction.builder()
                .id(1L)
                .tripId(300L)
                .passengerId(100L)
                .driverId(200L)
                .paymentMethod(mockPaymentMethod)
                .type(TransactionType.CHARGE)
                .amount(BigDecimal.valueOf(25.50))
                .currency("usd")
                .status(TransactionStatus.SUCCEEDED)
                .stripePaymentIntentId("pi_test123")
                .build();
    }

    @Test
    @DisplayName("Should create charge successfully")
    void shouldCreateCharge() {

        Long tripId = 300L;
        Long passengerId = 100L;
        Long driverId = 200L;
        BigDecimal amount = BigDecimal.valueOf(25.50);
        String currency = "usd";

        when(paymentMethodService.getDefaultForPassenger(passengerId))
                .thenReturn(mockPaymentMethod);
        when(stripeService.createPaymentIntent(
                amount, currency,
                mockPaymentMethod.getStripeCustomerId(),
                mockPaymentMethod.getStripePaymentMethodId(),
                tripId
        )).thenReturn(mockPaymentIntent);
        when(repository.save(any(Transaction.class))).thenReturn(mockTransaction);

        Transaction result = service.createCharge(tripId, passengerId, driverId, null, amount, currency);

        assertThat(result).isNotNull();
        assertThat(result.getType()).isEqualTo(TransactionType.CHARGE);
        assertThat(result.getStatus()).isEqualTo(TransactionStatus.SUCCEEDED);
        assertThat(result.getAmount()).isEqualTo(amount);

        verify(repository).save(argThat(transaction ->
                transaction.getType() == TransactionType.CHARGE &&
                        transaction.getStatus() == TransactionStatus.SUCCEEDED
        ));
    }

    @Test
    @DisplayName("Should throw exception when payment method is inactive")
    void shouldThrowExceptionWhenPaymentMethodInactive() {

        mockPaymentMethod.setActive(false);
        when(paymentMethodService.getDefaultForPassenger(100L))
                .thenReturn(mockPaymentMethod);

        assertThatThrownBy(() -> service.createCharge(
                300L, 100L, 200L, null, BigDecimal.valueOf(25.50), "usd"
        ))
                .isInstanceOf(PaymentProcessingException.class)
                .hasMessageContaining("not active");

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Should create refund successfully")
    void shouldCreateRefund() {

        Long transactionId = 1L;
        BigDecimal amount = BigDecimal.valueOf(25.50);
        String reason = "Trip cancelled";

        Refund mockRefund = new Refund();
        mockRefund.setId("re_test123");
        mockRefund.setStatus("succeeded");

        when(repository.findById(transactionId)).thenReturn(Optional.of(mockTransaction));
        when(stripeService.createRefund(mockTransaction.getStripePaymentIntentId(), amount))
                .thenReturn(mockRefund);
        when(repository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Transaction result = service.createRefund(transactionId, amount, reason);

        assertThat(result).isNotNull();
        assertThat(result.getType()).isEqualTo(TransactionType.REFUND);
        assertThat(result.getStatus()).isEqualTo(TransactionStatus.SUCCEEDED);

        verify(repository, times(2)).save(any(Transaction.class));
    }

    @Test
    @DisplayName("Should throw exception when refunding non-succeeded transaction")
    void shouldThrowExceptionWhenRefundingNonSucceeded() {

        mockTransaction.setStatus(TransactionStatus.PENDING);
        when(repository.findById(1L)).thenReturn(Optional.of(mockTransaction));

        assertThatThrownBy(() -> service.createRefund(1L, BigDecimal.valueOf(25.50), "reason"))
                .isInstanceOf(InvalidPaymentOperationException.class)
                .hasMessageContaining("only succeeded transactions");
    }

    @Test
    @DisplayName("Should throw exception when refund amount exceeds original")
    void shouldThrowExceptionWhenRefundAmountExceedsOriginal() {

        when(repository.findById(1L)).thenReturn(Optional.of(mockTransaction));

        assertThatThrownBy(() -> service.createRefund(1L, BigDecimal.valueOf(100.00), "reason"))
                .isInstanceOf(InvalidPaymentOperationException.class)
                .hasMessageContaining("cannot exceed");
    }

    @Test
    @DisplayName("Should create payout successfully")
    void shouldCreatePayout() {

        Long tripId = 300L;
        Long passengerId = 100L;
        Long driverId = 200L;
        BigDecimal amount = BigDecimal.valueOf(20.00);
        String currency = "usd";

        Transfer mockTransfer = new Transfer();
        mockTransfer.setId("tr_test123");

        when(driverPayoutAccountService.getVerifiedDefaultForDriver(driverId))
                .thenReturn(mockPayoutAccount);
        when(stripeService.createTransfer(
                amount, currency, mockPayoutAccount.getStripeAccountId(), tripId
        )).thenReturn(mockTransfer);
        when(repository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Transaction result = service.createPayout(tripId, passengerId, driverId, amount, currency);

        assertThat(result).isNotNull();
        assertThat(result.getType()).isEqualTo(TransactionType.PAYOUT);
        assertThat(result.getStatus()).isEqualTo(TransactionStatus.SUCCEEDED);
        assertThat(result.getAmount()).isEqualTo(amount);
    }

    @Test
    @DisplayName("Should get transaction by ID")
    void shouldGetById() {

        when(repository.findById(1L)).thenReturn(Optional.of(mockTransaction));

        Transaction result = service.getById(1L);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Should throw exception when transaction not found")
    void shouldThrowExceptionWhenNotFound() {

        when(repository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(999L))
                .isInstanceOf(TransactionNotFoundException.class);
    }

    @Test
    @DisplayName("Should get transaction by Stripe payment intent ID")
    void shouldGetByStripePaymentIntentId() {

        String intentId = "pi_test123";
        when(repository.findByStripePaymentIntentId(intentId))
                .thenReturn(Optional.of(mockTransaction));

        Transaction result = service.getByStripePaymentIntentId(intentId);

        assertThat(result).isNotNull();
        assertThat(result.getStripePaymentIntentId()).isEqualTo(intentId);
    }

    @Test
    @DisplayName("Should get transaction by trip ID")
    void shouldGetByTripId() {

        Long tripId = 300L;
        when(repository.findByTripId(tripId)).thenReturn(Optional.of(mockTransaction));

        Transaction result = service.getByTripId(tripId);

        assertThat(result).isNotNull();
        assertThat(result.getTripId()).isEqualTo(tripId);
    }

    @Test
    @DisplayName("Should get passenger history")
    void shouldGetPassengerHistory() {

        Long passengerId = 100L;
        when(repository.findRecentByPassengerId(passengerId))
                .thenReturn(List.of(mockTransaction));

        List<Transaction> result = service.getPassengerHistory(passengerId);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getPassengerId()).isEqualTo(passengerId);
    }

    @Test
    @DisplayName("Should get driver history")
    void shouldGetDriverHistory() {

        Long driverId = 200L;
        when(repository.findRecentByDriverId(driverId))
                .thenReturn(List.of(mockTransaction));

        List<Transaction> result = service.getDriverHistory(driverId);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getDriverId()).isEqualTo(driverId);
    }

    @Test
    @DisplayName("Should mark transaction as succeeded")
    void shouldMarkSucceeded() {

        String intentId = "pi_test123";
        mockTransaction.setStatus(TransactionStatus.PENDING);
        when(repository.findByStripePaymentIntentId(intentId))
                .thenReturn(Optional.of(mockTransaction));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.markSucceeded(intentId);

        verify(repository).save(argThat(t -> t.getStatus() == TransactionStatus.SUCCEEDED));
    }

    @Test
    @DisplayName("Should mark transaction as failed")
    void shouldMarkFailed() {

        String intentId = "pi_test123";
        mockTransaction.setStatus(TransactionStatus.PENDING);
        when(repository.findByStripePaymentIntentId(intentId))
                .thenReturn(Optional.of(mockTransaction));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.markFailed(intentId);

        verify(repository).save(argThat(t -> t.getStatus() == TransactionStatus.FAILED));
    }
}

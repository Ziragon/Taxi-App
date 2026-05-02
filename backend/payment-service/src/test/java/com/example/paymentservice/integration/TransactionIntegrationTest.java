package com.example.paymentservice.integration;

import com.example.paymentservice.entity.DriverPayoutAccount;
import com.example.paymentservice.entity.PaymentMethod;
import com.example.paymentservice.entity.Transaction;
import com.example.paymentservice.entity.enums.TransactionStatus;
import com.example.paymentservice.entity.enums.TransactionType;
import com.example.paymentservice.exception.InvalidPaymentOperationException;
import com.example.paymentservice.exception.TransactionNotFoundException;
import com.example.paymentservice.messaging.PaymentEventPublisher;
import com.example.paymentservice.repository.DriverPayoutAccountRepository;
import com.example.paymentservice.repository.PaymentMethodRepository;
import com.example.paymentservice.repository.TransactionRepository;
import com.example.paymentservice.service.StripeService;
import com.example.paymentservice.service.TransactionService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("Transaction Flow Integration Tests")
class TransactionIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private PaymentMethodRepository paymentMethodRepository;

    @Autowired
    private DriverPayoutAccountRepository payoutAccountRepository;

    @MockitoBean
    private PaymentEventPublisher paymentEventPublisher;

    @MockitoBean
    private StripeService stripeService;

    @Autowired
    private EntityManager entityManager;

    private PaymentMethod testPaymentMethod;
    private StripeService.FakePaymentIntent mockPaymentIntent; 

    @BeforeEach
    void setUp() {
        transactionRepository.deleteAll();
        payoutAccountRepository.deleteAll();
        paymentMethodRepository.deleteAll();

        testPaymentMethod = PaymentMethod.builder()
                .passengerId(100L)
                .stripeCustomerId("cus_test123")
                .stripePaymentMethodId("pm_test123")
                .cardBrand("visa")
                .lastFour("4242")
                .defaultvalue(true)
                .active(true)
                .build();
        paymentMethodRepository.save(testPaymentMethod);

        DriverPayoutAccount testPayoutAccount = DriverPayoutAccount.builder()
                .driverId(200L)
                .stripeAccountId("acct_test123")
                .lastFour("4242")
                .verified(true)
                .defaultvalue(true)
                .build();
        payoutAccountRepository.save(testPayoutAccount);

        mockPaymentIntent = new StripeService.FakePaymentIntent("pi_test123", "succeeded");

        when(stripeService.createPaymentIntent(any(), anyString(), anyString(), anyString(), any()))
                .thenReturn(mockPaymentIntent);
    }

    @Test
    @DisplayName("Полный flow: charge -> refund -> обновление статуса оригинала")
    void fullChargeAndRefundFlow() {
        Transaction charge = transactionService.createCharge(
                300L, 100L, 200L, null,
                BigDecimal.valueOf(25.50), "usd"
        );

        assertThat(charge).satisfies(t -> {
            assertThat(t.getTripId()).isEqualTo(300L);
            assertThat(t.getPassengerId()).isEqualTo(100L);
            assertThat(t.getDriverId()).isEqualTo(200L);
            assertThat(t.getType()).isEqualTo(TransactionType.CHARGE);
            assertThat(t.getStatus()).isEqualTo(TransactionStatus.SUCCEEDED);
            assertThat(t.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(25.50));
            assertThat(t.getCurrency()).isEqualTo("usd");
            assertThat(t.getStripePaymentIntentId()).isEqualTo("pi_test123");
        });

        assertThat(transactionRepository.count()).isEqualTo(1);

        StripeService.FakeRefund mockRefund = new StripeService.FakeRefund("re_test123", "succeeded");
        when(stripeService.createRefund(anyString(), any())).thenReturn(mockRefund);

        Transaction refund = transactionService.createRefund(
                charge.getId(), BigDecimal.valueOf(25.50), "Trip cancelled"
        );

        assertThat(refund).satisfies(r -> {
            assertThat(r.getType()).isEqualTo(TransactionType.REFUND);
            assertThat(r.getStatus()).isEqualTo(TransactionStatus.SUCCEEDED);
            assertThat(r.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(25.50));
            assertThat(r.getTripId()).isEqualTo(300L);
        });

        entityManager.flush();
        entityManager.clear();

        Transaction updatedCharge = transactionService.getById(charge.getId());
        assertThat(updatedCharge.getStatus()).isEqualTo(TransactionStatus.REFUNDED);

        assertThat(transactionRepository.count()).isEqualTo(2);
    }

    @Test
    @DisplayName("Charge с конкретным paymentMethodId")
    void chargeWithSpecificPaymentMethod() {
        Transaction charge = transactionService.createCharge(
                301L, 100L, 200L, testPaymentMethod.getId(),
                BigDecimal.valueOf(50.00), "usd"
        );

        assertThat(charge.getPaymentMethod().getId()).isEqualTo(testPaymentMethod.getId());
        assertThat(charge.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(50.00));
    }

    @Test
    @DisplayName("Частичный refund меньше суммы оригинала")
    void partialRefund() {
        Transaction charge = transactionService.createCharge(
                302L, 100L, 200L, null,
                BigDecimal.valueOf(100.00), "usd"
        );

        StripeService.FakeRefund mockRefund = new StripeService.FakeRefund("re_partial", "succeeded");
        when(stripeService.createRefund(anyString(), any())).thenReturn(mockRefund);

        Transaction refund = transactionService.createRefund(
                charge.getId(), BigDecimal.valueOf(30.00), "Partial refund"
        );

        assertThat(refund.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(30.00));
        assertThat(refund.getType()).isEqualTo(TransactionType.REFUND);
    }

    @Test
    @DisplayName("Нельзя сделать refund на сумму больше оригинала")
    void cannotRefundMoreThanOriginal() {
        Transaction charge = transactionService.createCharge(
                303L, 100L, 200L, null,
                BigDecimal.valueOf(25.50), "usd"
        );

        assertThatThrownBy(() ->
                transactionService.createRefund(
                        charge.getId(), BigDecimal.valueOf(100.00), "Too much"
                )
        ).isInstanceOf(InvalidPaymentOperationException.class)
                .hasMessageContaining("cannot exceed");
    }

    @Test
    @DisplayName("Нельзя сделать refund для транзакции не в статусе SUCCEEDED")
    void cannotRefundNonSucceededTransaction() {
        Transaction pendingTransaction = Transaction.builder()
                .tripId(304L)
                .passengerId(100L)
                .driverId(200L)
                .paymentMethod(testPaymentMethod)
                .type(TransactionType.CHARGE)
                .amount(BigDecimal.valueOf(25.50))
                .currency("usd")
                .status(TransactionStatus.PENDING)
                .stripePaymentIntentId("pi_pending")
                .build();
        transactionRepository.save(pendingTransaction);

        assertThatThrownBy(() ->
                transactionService.createRefund(
                        pendingTransaction.getId(), BigDecimal.valueOf(25.50), "reason"
                )
        ).isInstanceOf(InvalidPaymentOperationException.class)
                .hasMessageContaining("only succeeded transactions");
    }

    @Test
    @DisplayName("Payout flow: создание выплаты водителю")
    void payoutFlow() {
        StripeService.FakeTransfer mockTransfer = new StripeService.FakeTransfer("tr_test123");
        when(stripeService.createTransfer(any(), anyString(), anyString(), any()))
                .thenReturn(mockTransfer);

        Transaction payout = transactionService.createPayout(
                300L, 100L, 200L,
                BigDecimal.valueOf(20.00), "usd"
        );

        assertThat(payout).satisfies(p -> {
            assertThat(p.getType()).isEqualTo(TransactionType.PAYOUT);
            assertThat(p.getStatus()).isEqualTo(TransactionStatus.SUCCEEDED);
            assertThat(p.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(20.00));
            assertThat(p.getDriverId()).isEqualTo(200L);
            assertThat(p.getStripePaymentIntentId()).isEqualTo("tr_test123");
        });
    }

    @Test
    @DisplayName("markSucceeded обновляет статус транзакции")
    void markSucceededUpdatesStatus() {
        StripeService.FakePaymentIntent pendingIntent =
                new StripeService.FakePaymentIntent("pi_pending", "processing");
        when(stripeService.createPaymentIntent(any(), anyString(), anyString(), anyString(), any()))
                .thenReturn(pendingIntent);

        Transaction charge = transactionService.createCharge(
                305L, 100L, 200L, null,
                BigDecimal.valueOf(25.50), "usd"
        );

        transactionService.markSucceeded("pi_pending");

        entityManager.flush();
        entityManager.clear();

        Transaction updated = transactionService.getById(charge.getId());
        assertThat(updated.getStatus()).isEqualTo(TransactionStatus.SUCCEEDED);
    }

    @Test
    @DisplayName("markFailed обновляет статус транзакции")
    void markFailedUpdatesStatus() {
        Transaction charge = transactionService.createCharge(
                306L, 100L, 200L, null,
                BigDecimal.valueOf(25.50), "usd"
        );

        transactionService.markFailed("pi_test123");

        entityManager.flush();
        entityManager.clear();

        Transaction updated = transactionService.getById(charge.getId());
        assertThat(updated.getStatus()).isEqualTo(TransactionStatus.FAILED);
    }

    @Test
    @DisplayName("getByTripId возвращает транзакцию по ID поездки")
    void getByTripIdReturnsTransaction() {
        transactionService.createCharge(
                307L, 100L, 200L, null,
                BigDecimal.valueOf(25.50), "usd"
        );

        Transaction found = transactionService.getByTripId(307L);
        assertThat(found.getTripId()).isEqualTo(307L);
    }

    @Test
    @DisplayName("getPassengerHistory возвращает историю пассажира")
    void getPassengerHistoryReturnsTransactions() {
        when(stripeService.createPaymentIntent(any(), anyString(), anyString(), anyString(), any()))
                .thenReturn(mockPaymentIntent);

        transactionService.createCharge(310L, 100L, 200L, null, BigDecimal.valueOf(25.50), "usd");

        StripeService.FakePaymentIntent second = new StripeService.FakePaymentIntent("pi_second", "succeeded");
        when(stripeService.createPaymentIntent(any(), anyString(), anyString(), anyString(), any()))
                .thenReturn(second);

        transactionService.createCharge(311L, 100L, 200L, null, BigDecimal.valueOf(30.00), "usd");

        List<Transaction> history = transactionService.getPassengerHistory(100L);
        assertThat(history).hasSize(2);
        assertThat(history).allMatch(t -> t.getPassengerId().equals(100L));
    }

    @Test
    @DisplayName("getDriverHistory возвращает историю водителя")
    void getDriverHistoryReturnsTransactions() {
        transactionService.createCharge(320L, 100L, 200L, null, BigDecimal.valueOf(25.50), "usd");

        List<Transaction> history = transactionService.getDriverHistory(200L);
        assertThat(history).hasSize(1);
        assertThat(history.getFirst().getDriverId()).isEqualTo(200L);
    }

    @Test
    @DisplayName("getById выбрасывает исключение если транзакция не найдена")
    void getByIdThrowsWhenNotFound() {
        assertThatThrownBy(() ->
                transactionService.getById(999L)
        ).isInstanceOf(TransactionNotFoundException.class);
    }

    @Test
    @DisplayName("getByStripePaymentIntentId выбрасывает исключение если не найдена")
    void getByStripeIntentIdThrowsWhenNotFound() {
        assertThatThrownBy(() ->
                transactionService.getByStripePaymentIntentId("pi_nonexistent")
        ).isInstanceOf(TransactionNotFoundException.class);
    }
}
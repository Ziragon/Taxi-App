package com.example.paymentservice.service;

import com.example.paymentservice.entity.PaymentMethod;
import com.example.paymentservice.entity.Transaction;
import com.example.paymentservice.entity.enums.TransactionStatus;
import com.example.paymentservice.entity.enums.TransactionType;
import com.example.paymentservice.exception.InvalidPaymentOperationException;
import com.example.paymentservice.exception.PaymentProcessingException;
import com.example.paymentservice.exception.TransactionNotFoundException;
import com.example.paymentservice.messaging.PaymentEventPublisher;
import com.example.paymentservice.repository.TransactionRepository;
import com.example.shared.dto.event.PaymentFailedEvent;
import com.example.shared.dto.event.PaymentSucceededEvent;
import com.example.shared.dto.event.RefundSucceededEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final PaymentMethodService paymentMethodService;
    private final DriverPayoutAccountService driverPayoutAccountService;
    private final StripeService stripeService;
    private final PaymentEventPublisher paymentEventPublisher;

    @Transactional
    public Transaction createCharge(Long tripId,
                                    Long passengerId,
                                    Long driverId,
                                    Long paymentMethodId,
                                    BigDecimal amount,
                                    String currency) {
        PaymentMethod paymentMethod = paymentMethodId != null
                ? paymentMethodService.getById(paymentMethodId)
                : paymentMethodService.getDefaultForPassenger(passengerId);

        if (!paymentMethod.isActive()) {
            throw new PaymentProcessingException("Payment method is not active");
        }

        StripeService.FakePaymentIntent paymentIntent = stripeService.createPaymentIntent(
                amount,
                currency,
                paymentMethod.getStripeCustomerId(),
                paymentMethod.getStripePaymentMethodId(),
                tripId
        );

        TransactionStatus status = resolvePaymentIntentStatus(paymentIntent.status());

        Transaction transaction = Transaction.builder()
                .tripId(tripId)
                .passengerId(passengerId)
                .driverId(driverId)
                .paymentMethod(paymentMethod)
                .type(TransactionType.CHARGE)
                .amount(amount)
                .currency(currency)
                .status(status)
                .stripePaymentIntentId(paymentIntent.id())
                .build();

        Transaction saved = transactionRepository.save(transaction);

        log.info("Charge created for trip {} with status {}", tripId, status);

        if (status == TransactionStatus.SUCCEEDED) {
            paymentEventPublisher.publishPaymentSucceeded(
                    new PaymentSucceededEvent(
                            saved.getId(),
                            tripId,
                            passengerId,
                            driverId,
                            amount,
                            currency,
                            paymentIntent.id(),
                            Instant.now()
                    )
            );
        } else if (status == TransactionStatus.FAILED) {
            paymentEventPublisher.publishPaymentFailed(
                    new PaymentFailedEvent(
                            saved.getId(),
                            tripId,
                            passengerId,
                            driverId,
                            amount,
                            "Payment failed with status: " + paymentIntent.status(),
                            Instant.now()
                    )
            );
        }

        return saved;
    }

    @Transactional
    public Transaction createRefund(Long originalTransactionId,
                                    BigDecimal amount,
                                    String reason) {
        Transaction original = getById(originalTransactionId);

        if (original.getStatus() != TransactionStatus.SUCCEEDED) {
            throw new InvalidPaymentOperationException(
                    "Transaction",
                    "only succeeded transactions can be refunded"
            );
        }

        if (amount.compareTo(original.getAmount()) > 0) {
            throw new InvalidPaymentOperationException(
                    "Refund amount",
                    "cannot exceed original transaction amount"
            );
        }

        StripeService.FakeRefund stripeRefund = stripeService.createRefund(
                original.getStripePaymentIntentId(),
                amount
        );

        TransactionStatus refundStatus = resolveRefundStatus(stripeRefund.status());

        if (refundStatus == TransactionStatus.SUCCEEDED) {
            original.setStatus(TransactionStatus.REFUNDED);
            transactionRepository.save(original);
        }

        Transaction refund = Transaction.builder()
                .tripId(original.getTripId())
                .passengerId(original.getPassengerId())
                .driverId(original.getDriverId())
                .paymentMethod(original.getPaymentMethod())
                .type(TransactionType.REFUND)
                .amount(amount)
                .currency(original.getCurrency())
                .status(refundStatus)
                .stripePaymentIntentId(stripeRefund.id())
                .build();

        Transaction saved = transactionRepository.save(refund);

        log.info("Refund created for transaction {} with status {}", originalTransactionId, refundStatus);

        if (refundStatus == TransactionStatus.SUCCEEDED) {
            paymentEventPublisher.publishRefundSucceeded(
                    new RefundSucceededEvent(
                            saved.getId(),
                            originalTransactionId,
                            original.getTripId(),
                            original.getPassengerId(),
                            amount,
                            Instant.now()
                    )
            );
        }

        return saved;
    }

    @Transactional
    public Transaction createPayout(Long tripId,
                                    Long passengerId,
                                    Long driverId,
                                    BigDecimal amount,
                                    String currency) {
        var payoutAccount = driverPayoutAccountService.getVerifiedDefaultForDriver(driverId);

        StripeService.FakeTransfer transfer = stripeService.createTransfer(
                amount,
                currency,
                payoutAccount.getStripeAccountId(),
                tripId
        );

        Transaction payout = Transaction.builder()
                .tripId(tripId)
                .passengerId(passengerId)
                .driverId(driverId)
                .type(TransactionType.PAYOUT)
                .amount(amount)
                .currency(currency)
                .status(TransactionStatus.SUCCEEDED)
                .stripePaymentIntentId(transfer.id())
                .build();

        Transaction saved = transactionRepository.save(payout);

        log.info("Payout created for driver {} trip {}", driverId, tripId);

        return saved;
    }

    @Transactional
    public void updateStatus(Long id, TransactionStatus status) {
        Transaction transaction = getById(id);
        transaction.setStatus(status);
        transactionRepository.save(transaction);
    }

    @Transactional
    public void markSucceeded(String stripePaymentIntentId) {
        Transaction transaction = getByStripePaymentIntentId(stripePaymentIntentId);
        transaction.setStatus(TransactionStatus.SUCCEEDED);
        transactionRepository.save(transaction);
    }

    @Transactional
    public void markFailed(String stripePaymentIntentId) {
        Transaction transaction = getByStripePaymentIntentId(stripePaymentIntentId);
        transaction.setStatus(TransactionStatus.FAILED);
        transactionRepository.save(transaction);
    }

    @Transactional(readOnly = true)
    public Transaction getById(Long id) {
        return transactionRepository.findById(id)
                .orElseThrow(() -> new TransactionNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public Transaction getByStripePaymentIntentId(String stripePaymentIntentId) {
        return transactionRepository.findByStripePaymentIntentId(stripePaymentIntentId)
                .orElseThrow(() -> new TransactionNotFoundException(
                        "stripePaymentIntentId", stripePaymentIntentId
                ));
    }

    @Transactional(readOnly = true)
    public Transaction getByTripId(Long tripId) {
        return transactionRepository.findByTripId(tripId)
                .orElseThrow(() -> new TransactionNotFoundException(
                        "tripId", tripId
                ));
    }

    @Transactional(readOnly = true)
    public List<Transaction> getPassengerHistory(Long passengerId) {
        return transactionRepository.findRecentByPassengerId(passengerId);
    }

    @Transactional(readOnly = true)
    public List<Transaction> getDriverHistory(Long driverId) {
        return transactionRepository.findRecentByDriverId(driverId);
    }

    @Transactional(readOnly = true)
    public List<Transaction> getByStatus(TransactionStatus status) {
        return transactionRepository.findAllByStatus(status);
    }

    private TransactionStatus resolvePaymentIntentStatus(String stripeStatus) {
        return switch (stripeStatus) {
            case "succeeded" -> TransactionStatus.SUCCEEDED;
            case "processing" -> TransactionStatus.PENDING;
            case "requires_payment_method",
                 "requires_confirmation",
                 "requires_action" -> throw new PaymentProcessingException(
                    "Payment requires additional action: %s".formatted(stripeStatus)
            );
            default -> TransactionStatus.FAILED;
        };
    }

    private TransactionStatus resolveRefundStatus(String stripeStatus) {
        return switch (stripeStatus) {
            case "succeeded" -> TransactionStatus.SUCCEEDED;
            case "failed", "canceled" -> TransactionStatus.FAILED;
            default -> TransactionStatus.PENDING;
        };
    }
}
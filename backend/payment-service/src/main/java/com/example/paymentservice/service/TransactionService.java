package com.example.paymentservice.service;

import com.example.paymentservice.entity.Transaction;
import com.example.paymentservice.entity.enums.TransactionStatus;
import com.example.paymentservice.entity.enums.TransactionType;
import com.example.paymentservice.exception.InvalidPaymentOperationException;
import com.example.paymentservice.exception.TransactionNotFoundException;
import com.example.paymentservice.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final PaymentMethodService paymentMethodService;

    @Transactional
    public Transaction createCharge(Long tripId,
                                    Long passengerId,
                                    Long driverId,
                                    Long paymentMethodId,
                                    BigDecimal amount,
                                    String currency,
                                    String stripePaymentIntentId) {
        Transaction transaction = Transaction.builder()
                .tripId(tripId)
                .passengerId(passengerId)
                .driverId(driverId)
                .paymentMethod(paymentMethodService.getById(paymentMethodId))
                .type(TransactionType.CHARGE)
                .amount(amount)
                .currency(currency)
                .status(TransactionStatus.PENDING)
                .stripePaymentIntentId(stripePaymentIntentId)
                .build();

        return transactionRepository.save(transaction);
    }

    @Transactional
    public Transaction createRefund(Long originalTransactionId,
                                    BigDecimal amount,
                                    String stripePaymentIntentId) {
        Transaction original = getById(originalTransactionId);

        if (original.getStatus() != TransactionStatus.SUCCEEDED) {
            throw new InvalidPaymentOperationException(
                    "Transaction",
                    "only succeeded transactions can be refunded"
            );
        }

        Transaction refund = Transaction.builder()
                .tripId(original.getTripId())
                .passengerId(original.getPassengerId())
                .driverId(original.getDriverId())
                .paymentMethod(original.getPaymentMethod())
                .type(TransactionType.REFUND)
                .amount(amount)
                .currency(original.getCurrency())
                .status(TransactionStatus.PENDING)
                .stripePaymentIntentId(stripePaymentIntentId)
                .build();

        return transactionRepository.save(refund);
    }

    @Transactional
    public Transaction createPayout(Long tripId,
                                    Long passengerId,
                                    Long driverId,
                                    BigDecimal amount,
                                    String currency,
                                    String stripePaymentIntentId) {
        Transaction payout = Transaction.builder()
                .tripId(tripId)
                .passengerId(passengerId)
                .driverId(driverId)
                .type(TransactionType.PAYOUT)
                .amount(amount)
                .currency(currency)
                .status(TransactionStatus.PENDING)
                .stripePaymentIntentId(stripePaymentIntentId)
                .build();

        return transactionRepository.save(payout);
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
}

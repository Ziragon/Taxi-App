package com.example.paymentservice.service;

import com.example.paymentservice.config.StripeProperties;
import com.example.paymentservice.exception.StripeException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class StripeService {

    private final StripeProperties stripeProperties;

    public FakeCustomer getOrCreateCustomer(Long passengerId) {
        String fakeCustomerId = "cus_fake_" + passengerId;
        log.info("[STUB] getOrCreateCustomer for passenger {}: {}", passengerId, fakeCustomerId);
        return new FakeCustomer(fakeCustomerId);
    }


    public FakePaymentMethod attachPaymentMethodToCustomer(String paymentMethodId, String customerId) {
        log.info("[STUB] attachPaymentMethodToCustomer: pm={}, customer={}", paymentMethodId, customerId);
        return new FakePaymentMethod(paymentMethodId, "visa", "4242");
    }

    public void setDefaultPaymentMethod(String customerId, String paymentMethodId) {
        log.info("[STUB] setDefaultPaymentMethod: customer={}, pm={}", customerId, paymentMethodId);
    }

    public void detachPaymentMethod(String paymentMethodId) {
        log.info("[STUB] detachPaymentMethod: pm={}", paymentMethodId);
    }

    public FakePaymentIntent createPaymentIntent(
            BigDecimal amount,
            String currency,
            String customerId,
            String paymentMethodId,
            Long tripId
    ) {
        String fakeId = "pi_fake_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        log.info("[STUB] createPaymentIntent: trip={}, amount={} {}, pi={}", tripId, amount, currency, fakeId);
        return new FakePaymentIntent(fakeId, "succeeded");
    }

    public FakeRefund createRefund(String paymentIntentId, BigDecimal amount) {
        String fakeId = "re_fake_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        log.info("[STUB] createRefund: pi={}, amount={}, refund={}", paymentIntentId, amount, fakeId);
        return new FakeRefund(fakeId, "succeeded");
    }

    public FakeAccount getOrCreateConnectAccount(Long driverId) {
        String fakeId = "acct_fake_" + driverId;
        log.info("[STUB] getOrCreateConnectAccount for driver {}: {}", driverId, fakeId);
        return new FakeAccount(fakeId);
    }

    public boolean isAccountVerified(String accountId) {
        log.info("[STUB] isAccountVerified: account={}", accountId);
        return true;
    }

    public FakeTransfer createTransfer(
            BigDecimal amount,
            String currency,
            String destinationAccountId,
            Long tripId
    ) {
        String fakeId = "tr_fake_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        log.info("[STUB] createTransfer: trip={}, amount={} {}, transfer={}", tripId, amount, currency, fakeId);
        return new FakeTransfer(fakeId);
    }

    public record FakeCustomer(String id) {}

    public record FakePaymentMethod(String id, String brand, String last4) {}

    public record FakePaymentIntent(String id, String status) {}

    public record FakeRefund(String id, String status) {}

    public record FakeAccount(String id) {}

    public record FakeTransfer(String id) {}
}
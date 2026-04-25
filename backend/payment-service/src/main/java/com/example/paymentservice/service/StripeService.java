package com.example.paymentservice.service;

import com.example.paymentservice.config.StripeProperties;
import com.example.paymentservice.exception.StripeException;
import com.stripe.model.*;
import com.stripe.param.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class StripeService {

    private final StripeProperties stripeProperties;

    public Customer getOrCreateCustomer(Long passengerId) {
        try {
            CustomerSearchParams searchParams = CustomerSearchParams.builder()
                    .setQuery("metadata['passenger_id']:'" + passengerId + "'")
                    .build();

            CustomerSearchResult result = Customer.search(searchParams);

            if (!result.getData().isEmpty()) {
                return result.getData().getFirst();
            }

            Map<String, String> metadata = new HashMap<>();
            metadata.put("passenger_id", passengerId.toString());

            CustomerCreateParams params = CustomerCreateParams.builder()
                    .putAllMetadata(metadata)
                    .build();

            return Customer.create(params);

        } catch (com.stripe.exception.StripeException e) {
            log.error("Failed to get or create Stripe customer for passenger {}: {}", passengerId, e.getMessage());
            throw new StripeException("Failed to create Stripe customer", e);
        }
    }

    public PaymentMethod attachPaymentMethodToCustomer(String paymentMethodId, String customerId) {
        try {
            PaymentMethod paymentMethod = PaymentMethod.retrieve(paymentMethodId);

            PaymentMethodAttachParams params = PaymentMethodAttachParams.builder()
                    .setCustomer(customerId)
                    .build();

            return paymentMethod.attach(params);

        } catch (com.stripe.exception.StripeException e) {
            log.error("Failed to attach payment method {} to customer {}: {}",
                    paymentMethodId, customerId, e.getMessage());
            throw new StripeException("Failed to attach payment method", e);
        }
    }

    public PaymentMethod getPaymentMethodDetails(String paymentMethodId) {
        try {
            return PaymentMethod.retrieve(paymentMethodId);
        } catch (com.stripe.exception.StripeException e) {
            log.error("Failed to retrieve payment method {}: {}", paymentMethodId, e.getMessage());
            throw new StripeException("Failed to retrieve payment method", e);
        }
    }

    public void setDefaultPaymentMethod(String customerId, String paymentMethodId) {
        try {
            Customer customer = Customer.retrieve(customerId);

            CustomerUpdateParams params = CustomerUpdateParams.builder()
                    .setInvoiceSettings(
                            CustomerUpdateParams.InvoiceSettings.builder()
                                    .setDefaultPaymentMethod(paymentMethodId)
                                    .build()
                    )
                    .build();

            customer.update(params);

        } catch (com.stripe.exception.StripeException e) {
            log.error("Failed to set default payment method for customer {}: {}", customerId, e.getMessage());
            throw new StripeException("Failed to set default payment method", e);
        }
    }

    public void detachPaymentMethod(String paymentMethodId) {
        try {
            PaymentMethod paymentMethod = PaymentMethod.retrieve(paymentMethodId);
            paymentMethod.detach();

        } catch (com.stripe.exception.StripeException e) {
            log.error("Failed to detach payment method {}: {}", paymentMethodId, e.getMessage());
            throw new StripeException("Failed to detach payment method", e);
        }
    }

    public PaymentIntent createPaymentIntent(
            BigDecimal amount,
            String currency,
            String customerId,
            String paymentMethodId,
            Long tripId
    ) {
        try {
            long amountInCents = amount.multiply(BigDecimal.valueOf(100)).longValue();

            Map<String, String> metadata = new HashMap<>();
            metadata.put("trip_id", tripId.toString());

            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                    .setAmount(amountInCents)
                    .setCurrency(currency.toLowerCase())
                    .setCustomer(customerId)
                    .setPaymentMethod(paymentMethodId)
                    .setConfirm(true)
                    .setAutomaticPaymentMethods(
                            PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                                    .setEnabled(true)
                                    .setAllowRedirects(PaymentIntentCreateParams.AutomaticPaymentMethods.AllowRedirects.NEVER)
                                    .build()
                    )
                    .putAllMetadata(metadata)
                    .build();

            return PaymentIntent.create(params);

        } catch (com.stripe.exception.StripeException e) {
            log.error("Failed to create payment intent for trip {}: {}", tripId, e.getMessage());
            throw new StripeException("Failed to create payment", e);
        }
    }

    public Refund createRefund(String paymentIntentId, BigDecimal amount) {
        try {
            long amountInCents = amount.multiply(BigDecimal.valueOf(100)).longValue();

            RefundCreateParams params = RefundCreateParams.builder()
                    .setPaymentIntent(paymentIntentId)
                    .setAmount(amountInCents)
                    .build();

            return Refund.create(params);

        } catch (com.stripe.exception.StripeException e) {
            log.error("Failed to create refund for payment intent {}: {}", paymentIntentId, e.getMessage());
            throw new StripeException("Failed to create refund", e);
        }
    }

    public Account getOrCreateConnectAccount(Long driverId) {
        try {
            AccountListParams listParams = AccountListParams.builder().build();
            AccountCollection accounts = Account.list(listParams);

            for (Account account : accounts.getData()) {
                if (account.getMetadata() != null &&
                        driverId.toString().equals(account.getMetadata().get("driver_id"))) {
                    return account;
                }
            }

            Map<String, String> metadata = new HashMap<>();
            metadata.put("driver_id", driverId.toString());

            AccountCreateParams params = AccountCreateParams.builder()
                    .setType(AccountCreateParams.Type.EXPRESS)
                    .setCapabilities(
                            AccountCreateParams.Capabilities.builder()
                                    .setCardPayments(
                                            AccountCreateParams.Capabilities.CardPayments.builder()
                                                    .setRequested(true)
                                                    .build()
                                    )
                                    .setTransfers(
                                            AccountCreateParams.Capabilities.Transfers.builder()
                                                    .setRequested(true)
                                                    .build()
                                    )
                                    .build()
                    )
                    .putAllMetadata(metadata)
                    .build();

            return Account.create(params);

        } catch (com.stripe.exception.StripeException e) {
            log.error("Failed to get or create Stripe Connect account for driver {}: {}", driverId, e.getMessage());
            throw new StripeException("Failed to create Stripe Connect account", e);
        }
    }

    public boolean isAccountVerified(String accountId) {
        try {
            Account account = Account.retrieve(accountId);
            return account.getChargesEnabled() && account.getPayoutsEnabled();

        } catch (com.stripe.exception.StripeException e) {
            log.error("Failed to check verification status for account {}: {}", accountId, e.getMessage());
            throw new StripeException("Failed to check account verification", e);
        }
    }

    public Transfer createTransfer(
            BigDecimal amount,
            String currency,
            String destinationAccountId,
            Long tripId
    ) {
        try {
            long amountInCents = amount.multiply(BigDecimal.valueOf(100)).longValue();

            Map<String, String> metadata = new HashMap<>();
            metadata.put("trip_id", tripId.toString());

            TransferCreateParams params = TransferCreateParams.builder()
                    .setAmount(amountInCents)
                    .setCurrency(currency.toLowerCase())
                    .setDestination(destinationAccountId)
                    .putAllMetadata(metadata)
                    .build();

            return Transfer.create(params);

        } catch (com.stripe.exception.StripeException e) {
            log.error("Failed to create transfer for trip {}: {}", tripId, e.getMessage());
            throw new StripeException("Failed to create transfer", e);
        }
    }
}
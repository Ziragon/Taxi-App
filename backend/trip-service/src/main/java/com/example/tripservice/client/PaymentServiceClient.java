package com.example.tripservice.client;

import com.example.shared.security.InternalFeignConfig;
import com.example.tripservice.dto.client.*;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(
        name = "payment-service",
        url = "${internal.payment-service}",
        configuration = InternalFeignConfig.class
)
public interface PaymentServiceClient {

    @GetMapping("/api/v1/internal/payment-methods/passenger/{passengerId}/default")
    PaymentMethodResponse getDefaultPaymentMethod(@PathVariable Long passengerId);

    @GetMapping("/api/v1/internal/payout-accounts/driver/{driverId}/has-verified")
    Boolean hasVerifiedPayoutAccount(@PathVariable Long driverId);

    @PostMapping("/api/v1/internal/transactions/charge")
    TransactionResponse createCharge(@RequestBody CreateChargeRequest request);

    @PostMapping("/api/v1/internal/transactions/refund")
    TransactionResponse createRefund(@RequestBody CreateRefundRequest request);

    @PostMapping("/api/v1/internal/transactions/payout")
    TransactionResponse createPayout(@RequestBody CreatePayoutRequest request);

    @GetMapping("/api/v1/internal/transactions/trip/{tripId}")
    TransactionResponse getTransactionByTripId(@PathVariable Long tripId);
}
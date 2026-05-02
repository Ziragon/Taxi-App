package com.example.tripservice.dto.client;

import java.math.BigDecimal;
import java.time.Instant;

public record TransactionResponse(
        Long id,
        Long tripId,
        Long passengerId,
        Long driverId,
        String type,
        String status,
        BigDecimal amount,
        String currency,
        String stripePaymentIntentId,
        Instant createdAt
) {}

package com.example.shared.dto.event;

import java.math.BigDecimal;
import java.time.Instant;

public record PayoutSucceededEvent(
        Long payoutTransactionId,
        Long tripId,
        Long passengerId,
        Long driverId,
        BigDecimal amount,
        String currency,
        String stripeTransferId,
        Instant occurredAt
) {}
package com.example.paymentservice.dto.response;

import java.math.BigDecimal;
import java.time.Instant;

public record TripResponse(
        Long id,
        Long passengerId,
        Long driverId,
        String status,
        BigDecimal amount,
        String currency,
        Instant createdAt
) {}

package com.example.tripservice.dto.client;

import java.math.BigDecimal;

public record CreatePayoutRequest(
        Long tripId,
        Long passengerId,
        Long driverId,
        BigDecimal amount,
        String currency
) {}

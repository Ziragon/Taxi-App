package com.example.tripservice.dto.client;

import java.math.BigDecimal;

public record CreateChargeRequest(
        Long tripId,
        Long passengerId,
        Long driverId,
        Long paymentMethodId,
        BigDecimal amount,
        String currency
) {}
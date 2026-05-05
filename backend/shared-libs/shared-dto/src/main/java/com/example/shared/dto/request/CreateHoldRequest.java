package com.example.shared.dto.request;

import java.math.BigDecimal;

public record CreateHoldRequest(
        Long tripId,
        Long passengerId,
        Long driverId,
        Long paymentMethodId,
        BigDecimal amount,
        String currency
) {}

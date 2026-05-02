package com.example.tripservice.dto.client;

import java.time.Instant;

public record PaymentMethodResponse(
        Long id,
        Long passengerId,
        String cardBrand,
        String lastFour,
        boolean isDefault,
        boolean isActive,
        Instant createdAt
) {}

package com.example.notificationservice.dto;

import java.math.BigDecimal;

public record DriverProfileSnapshot(
        Long accountId,
        String firstName,
        String lastName,
        String photoUrl,
        BigDecimal averageRating
) {}

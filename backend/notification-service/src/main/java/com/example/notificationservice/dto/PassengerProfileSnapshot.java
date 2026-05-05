package com.example.notificationservice.dto;

import java.math.BigDecimal;

public record PassengerProfileSnapshot(
        Long accountId,
        String firstName,
        String lastName,
        String photoUrl,
        BigDecimal averageRating
) {}

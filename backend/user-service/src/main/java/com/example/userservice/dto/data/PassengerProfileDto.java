package com.example.userservice.dto.data;

import java.math.BigDecimal;

public record PassengerProfileDto(
        Long accountId,
        String firstName,
        String lastName,
        String photoUrl,
        BigDecimal averageRating,
        Integer totalTrips
) {}
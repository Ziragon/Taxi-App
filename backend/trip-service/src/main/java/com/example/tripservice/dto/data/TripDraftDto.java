package com.example.tripservice.dto.data;

import java.math.BigDecimal;

public record TripDraftDto(

        Long passengerId,

        String originAddress,

        BigDecimal originLat,

        BigDecimal originLng,

        String destinationAddress,

        BigDecimal destinationLat,

        BigDecimal destinationLng,

        BigDecimal distanceKm,

        BigDecimal durationMin,

        BigDecimal weatherCoef,

        BigDecimal surgeCoef
) {}

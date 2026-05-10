package com.example.tripservice.dto.data;

import com.example.tripservice.entity.Trip;
import com.example.tripservice.entity.enums.TripStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record TripSummaryDto(

        Long id,

        TripStatus status,

        String originAddress,

        String destAddress,

        BigDecimal distanceKm,

        BigDecimal price,

        Instant createdAt
) {
    public static TripSummaryDto from(Trip trip) {
        return new TripSummaryDto(
                trip.getId(),
                trip.getStatus(),
                trip.getOriginAddress(),
                trip.getDestinationAddress(),
                trip.getDistanceKm(),
                trip.getPrice(),
                trip.getCreatedAt()
        );
    }
}
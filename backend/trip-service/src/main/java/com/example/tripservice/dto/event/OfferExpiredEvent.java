package com.example.tripservice.dto.event;

public record OfferExpiredEvent(
        Long tripId,
        Long driverId
) {}

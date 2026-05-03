package com.example.shared.dto.event;

import com.example.shared.dto.enums.VehicleClass;
import java.math.BigDecimal;

public record TripOfferEvent(
        Long tripId,
        Long driverId,
        String originAddress,
        BigDecimal originLat,
        BigDecimal originLng,
        String destinationAddress,
        BigDecimal destinationLat,
        BigDecimal destinationLng,
        BigDecimal price,
        BigDecimal distanceKm,
        BigDecimal durationMin,
        VehicleClass vehicleClass
) {}
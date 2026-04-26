package com.example.tripservice.dto.response;

import com.example.tripservice.dto.data.TripDto;
import com.example.tripservice.entity.enums.TripStatus;

import java.math.BigDecimal;

public record TripResponse(
        Long passengerId,

        TripStatus status,

        String originAddress,

        BigDecimal originLat,

        BigDecimal originLng,

        String destAddress,

        BigDecimal destLat,

        BigDecimal destLng,

        BigDecimal distanceKm,

        Integer durationSec,

        BigDecimal weatherCoef,

        BigDecimal surgeCoef
) {
    public static TripResponse from(TripDto trip) {
        return new TripResponse(
                trip.passengerId(),
                trip.status(),
                trip.originAddress(),
                trip.originLat(),
                trip.originLng(),
                trip.destAddress(),
                trip.destLat(),
                trip.destLng(),
                trip.distanceKm(),
                trip.durationSec(),
                trip.weatherCoef(),
                trip.surgeCoef()
        );
    }
}

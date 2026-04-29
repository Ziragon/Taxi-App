package com.example.tripservice.dto.response;

import com.example.tripservice.dto.data.TripDto;
import com.example.tripservice.entity.enums.TripStatus;

import java.math.BigDecimal;
import java.util.List;

public record TripResponse(

        Long id,

        Long passengerId,

        TripStatus status,

        String originAddress,

        BigDecimal originLat,

        BigDecimal originLng,

        String destAddress,

        BigDecimal destLat,

        BigDecimal destLng,

        BigDecimal distanceKm,

        BigDecimal durationMin,

        BigDecimal weatherCoef,

        BigDecimal surgeCoef,

        List<TariffResponse> tariffs,

        String routeGeometry
) {
    public static TripResponse from(TripDto trip) {
        List<TariffResponse> tariffs = trip.tariffDtos() == null
                ? List.of()
                : trip.tariffDtos().stream()
                  .map(TariffResponse::from)
                  .toList();

        return new TripResponse(
                trip.id(),
                trip.passengerId(),
                trip.status(),
                trip.originAddress(),
                trip.originLat(),
                trip.originLng(),
                trip.destAddress(),
                trip.destLat(),
                trip.destLng(),
                trip.distanceKm(),
                trip.durationMin(),
                trip.weatherCoef(),
                trip.surgeCoef(),
                tariffs,
                trip.routeGeometry()
        );
    }
}

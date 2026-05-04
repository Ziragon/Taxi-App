package com.example.tripservice.dto.data;

import com.example.tripservice.entity.Trip;
import com.example.tripservice.entity.enums.TripStatus;

import java.math.BigDecimal;
import java.util.List;

public record TripDto(

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

        List<TariffDto> tariffDtos,

        String routeGeometry
) {
    public static TripDto from(Trip trip, List<TariffDto> dtos, String route) {
        return new TripDto(
                trip.getId(),
                trip.getPassengerId(),
                trip.getStatus(),
                trip.getOriginAddress(),
                trip.getOriginLat(),
                trip.getOriginLng(),
                trip.getDestinationAddress(),
                trip.getDestinationLat(),
                trip.getDestinationLng(),
                trip.getDistanceKm(),
                trip.getDurationMin(),
                trip.getWeatherCoef(),
                trip.getSurgeCoef(),
                dtos,
                route
        );
    }

    public static TripDto fromDraft(TripDraftDto draft, List<TariffDto> dtos, String route) {
        return new TripDto(
                null,
                draft.passengerId(),
                null,
                draft.originAddress(),
                draft.originLat(),
                draft.originLng(),
                draft.destinationAddress(),
                draft.destinationLat(),
                draft.destinationLng(),
                draft.distanceKm(),
                draft.durationMin(),
                draft.weatherCoef(),
                draft.surgeCoef(),
                dtos,
                route
        );
    }
}

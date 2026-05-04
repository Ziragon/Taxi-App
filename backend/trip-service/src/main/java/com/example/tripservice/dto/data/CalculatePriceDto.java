package com.example.tripservice.dto.data;

import com.example.tripservice.entity.Tariff;

import java.math.BigDecimal;

public record CalculatePriceDto(

        BigDecimal baseFare,

        BigDecimal minFare,

        BigDecimal distanceKm,

        BigDecimal durationMin,

        BigDecimal pricePerKm,

        BigDecimal pricePerMin,

        BigDecimal weatherCoef,

        BigDecimal surgeCoef
) {
    public static CalculatePriceDto from(Tariff tariff, TripDraftDto trip) {
        return new CalculatePriceDto(
                tariff.getBaseFare(),
                tariff.getMinFare(),
                trip.distanceKm(),
                trip.durationMin(),
                tariff.getPricePerKm(),
                tariff.getPricePerMin(),
                trip.weatherCoef(),
                trip.surgeCoef()
        );
    }

    public static CalculatePriceDto from(Tariff tariff, TripDto trip) {
        return new CalculatePriceDto(
                tariff.getBaseFare(),
                tariff.getMinFare(),
                trip.distanceKm(),
                trip.durationMin(),
                tariff.getPricePerKm(),
                tariff.getPricePerMin(),
                trip.weatherCoef(),
                trip.surgeCoef()
        );
    }
}

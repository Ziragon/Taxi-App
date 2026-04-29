package com.example.tripservice.dto.data;

import com.example.tripservice.entity.Trip;

import java.math.BigDecimal;

public record PriceBreakdown (

        BigDecimal baseFare,

        BigDecimal kmCost,

        BigDecimal minCost,

        BigDecimal weatherCoef,

        BigDecimal surgeCoef
) {
    public static PriceBreakdown from(Trip trip, TariffDto tariffDto) {
        return new PriceBreakdown(
                tariffDto.baseFare(),
                tariffDto.prices().distanceCost(),
                tariffDto.prices().timeCost(),
                trip.getWeatherCoef(),
                trip.getSurgeCoef()
        );
    }
}

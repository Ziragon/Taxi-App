package com.example.tripservice.dto.response;

import com.example.tripservice.dto.data.TariffDto;
import com.example.tripservice.entity.enums.VehicleClass;

import java.math.BigDecimal;

public record TariffResponse(

        Long id,

        VehicleClass tripClass,

        BigDecimal baseFare,

        BigDecimal pricePerKm,

        BigDecimal distanceCost,

        BigDecimal pricePerMin,

        BigDecimal durationCost,

        BigDecimal price,

        Integer carsNearby
) {
    public static TariffResponse from(TariffDto dto) {
        return new TariffResponse(
                dto.id(),
                dto.tripClass(),
                dto.baseFare(),
                dto.pricePerKm(),
                dto.prices().distanceCost(),
                dto.pricePerMin(),
                dto.prices().timeCost(),
                dto.prices().price(),
                null // TODO Заглушка
        );
    }
}

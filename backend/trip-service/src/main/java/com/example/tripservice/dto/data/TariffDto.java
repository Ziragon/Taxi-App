package com.example.tripservice.dto.data;

import com.example.tripservice.entity.Tariff;
import com.example.tripservice.entity.enums.VehicleClass;

import java.math.BigDecimal;

public record TariffDto(

        Long id,

        VehicleClass tripClass,

        BigDecimal baseFare,

        BigDecimal pricePerKm,

        BigDecimal pricePerMin,

        BigDecimal price
) {
    public static TariffDto from(Tariff tariff, BigDecimal price) {
        return new TariffDto(
                tariff.getId(),
                tariff.getTripClass(),
                tariff.getBaseFare(),
                tariff.getPricePerKm(),
                tariff.getPricePerMin(),
                price
        );
    }
}

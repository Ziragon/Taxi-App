package com.example.tripservice.dto.data;

import com.example.tripservice.entity.Tariff;
import com.example.shared.dto.enums.VehicleClass;

import java.math.BigDecimal;

public record TariffDto(

        Long id,

        VehicleClass tripClass,

        BigDecimal baseFare,

        BigDecimal pricePerKm,

        BigDecimal pricePerMin,

        TariffPriceData prices,

        Integer driversNearby
) {
    public static TariffDto from(Tariff tariff, TariffPriceData prices, Integer driversNearby) {
        return new TariffDto(
                tariff.getId(),
                tariff.getTripClass(),
                tariff.getBaseFare(),
                tariff.getPricePerKm(),
                tariff.getPricePerMin(),
                prices,
                driversNearby
        );
    }
}

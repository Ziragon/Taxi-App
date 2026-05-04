package com.example.tripservice.dto.response;

import com.example.tripservice.dto.data.TariffDto;
import com.example.shared.dto.enums.VehicleClass;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

public record TariffResponse(

        @Schema(description = "ID тарифа", example = "1")
        Long id,

        @Schema(description = "Класс авто", example = "COMFORT")
        VehicleClass tripClass,

        @Schema(description = "Базовая цена (посадка)", example = "150.00")
        BigDecimal baseFare,

        @Schema(description = "Цена за 1 км", example = "15.00")
        BigDecimal pricePerKm,

        @Schema(description = "Итоговая стоимость за дистанцию", example = "225.00")
        BigDecimal distanceCost,

        @Schema(description = "Цена за 1 мин", example = "6.00")
        BigDecimal pricePerMin,

        @Schema(description = "Итоговая стоимость за время", example = "120.00")
        BigDecimal durationCost,

        @Schema(description = "Общая стоимость поездки", example = "495.00")
        BigDecimal price,

        @Schema(description = "Количество свободных машин рядом", example = "5")
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
                dto.driversNearby()
        );
    }
}

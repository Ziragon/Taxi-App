package com.example.tripservice.dto.response;

import com.example.tripservice.dto.data.RouteDto;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.math.RoundingMode;

public record RouteResponse(

        @Schema(description = "Длительность поездки в минутах", example = "25.5")
        BigDecimal durationMin,

        @Schema(description = "Дистанция в километрах", example = "12.450")
        BigDecimal distanceKm,

        @Schema(description = "Геометрия маршрута в формате Polyline6", example = "a~l~Fjk~uOn...")
        String geometry
) {
    public static RouteResponse from(RouteDto dto) {
        return new RouteResponse(
                BigDecimal.valueOf(dto.duration()).divide(new BigDecimal("60"), 2, RoundingMode.HALF_UP),
                BigDecimal.valueOf(dto.distance()).divide(new BigDecimal("1000"), 3, RoundingMode.HALF_UP),
                dto.geometry()
        );
    }
}

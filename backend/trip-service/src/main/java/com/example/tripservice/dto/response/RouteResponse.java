package com.example.tripservice.dto.response;

import com.example.tripservice.dto.data.RouteDto;

import java.math.BigDecimal;
import java.math.RoundingMode;

public record RouteResponse(

        BigDecimal durationMin,

        BigDecimal distanceKm,

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

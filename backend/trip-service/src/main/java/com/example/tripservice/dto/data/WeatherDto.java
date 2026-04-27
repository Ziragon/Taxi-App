package com.example.tripservice.dto.data;

import com.example.tripservice.dto.response.WeatherResponse;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record WeatherDto(
        String location,

        LocalDateTime localtime,

        Double tempC,

        String weatherCondition,

        Integer weatherCode,

        BigDecimal weatherCoef
) {
    public static WeatherDto from(WeatherResponse response, BigDecimal weatherCoef) {

        return new WeatherDto(
                response.location().name(),
                response.location().localtime(),
                response.current().tempC(),
                response.current().condition().text(),
                response.current().condition().code(),
                weatherCoef
        );
    }
}

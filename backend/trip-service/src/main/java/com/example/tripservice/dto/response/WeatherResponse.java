package com.example.tripservice.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

public record WeatherResponse(

        Location location,

        Current current
) {
    public record Location(
            String name,

            @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm")
            LocalDateTime localtime
    ) {}

    public record Current(
            @JsonProperty("temp_c") Double tempC,

            Condition condition
    ) {}

    public record Condition(
            String text,

            Integer code
    ) {}
}
package com.example.tripservice.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record DriverCoordinatesRequest(

        @NotNull(message = "Координаты обязательны")
        @Schema(description = "Широта водителя (Нск, пл. Ленина)", example = "55.0302")
        BigDecimal latitude,

        @NotNull(message = "Координаты обязательны")
        @Schema(description = "Долгота водителя (Нск, пл. Ленина)", example = "82.9204")
        BigDecimal longitude
) {}
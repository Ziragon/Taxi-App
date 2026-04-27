package com.example.tripservice.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record TripCreateRequest(

        @NotBlank(message = "Адрес отправления обязателен")
        @Schema(description = "Адрес пользователя", example = "Новосибирск, 52")
        String originAddress,

        @NotNull(message = "Координаты обязательны")
        @Schema(description = "Широта координат пользователя", example = "54.8427")
        BigDecimal originLat,

        @NotNull(message = "Координаты обязательны")
        @Schema(description = "Долгота координат пользователя", example = "83.0916")
        BigDecimal originLng,

        @NotBlank(message = "Адрес назначения обязателен")
        @Schema(description = "Адрес назначения", example = "Новосибирск, 42")
        String destAddress,

        @NotNull(message = "Координаты обязательны")
        @Schema(description = "Широта координат пользователя", example = "55.0302")
        BigDecimal destLat,

        @NotNull(message = "Координаты обязательны")
        @Schema(description = "Долгота координат пользователя", example = "82.9366")
        BigDecimal destLng
) {}

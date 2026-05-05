package com.example.shared.dto.event;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;

@Schema(description = "Событие: поездка завершена")
public record TripInProgressEvent(

        @Schema(description = "ID поездки", example = "123")
        Long tripId,

        @Schema(description = "ID пассажира", example = "456")
        Long passengerId,

        @Schema(description = "ID водителя", example = "789")
        Long driverId,

        @Schema(description = "Сумма к оплате", example = "25.50")
        BigDecimal amount,

        @Schema(description = "Валюта", example = "USD")
        String currency,

        @Schema(description = "Время завершения поездки")
        Instant completedAt
) {}
package com.example.shared.dto.event;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;

@Schema(description = "Событие: платёж провален")
public record PaymentFailedEvent(

        @Schema(description = "ID транзакции", example = "1")
        Long transactionId,

        @Schema(description = "ID поездки", example = "123")
        Long tripId,

        @Schema(description = "ID пассажира", example = "456")
        Long passengerId,

        @Schema(description = "ID водителя", example = "789")
        Long driverId,

        @Schema(description = "Сумма", example = "25.50")
        BigDecimal amount,

        @Schema(description = "Причина отказа", example = "Insufficient funds")
        String reason,

        @Schema(description = "Время провала платежа")
        Instant failedAt
) {}

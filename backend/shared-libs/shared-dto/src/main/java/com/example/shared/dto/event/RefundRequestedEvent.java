package com.example.shared.dto.event;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Событие: запрос на возврат средств")
public record RefundRequestedEvent(

        @Schema(description = "ID поездки", example = "123")
        Long tripId,

        @Schema(description = "ID пассажира", example = "456")
        Long passengerId,

        @Schema(description = "ID водителя", example = "789")
        Long driverId,

        @Schema(description = "Сумма к возврату", example = "25.50")
        BigDecimal amount,

        @Schema(description = "Причина возврата", example = "Trip cancelled by driver")
        String reason
) {}

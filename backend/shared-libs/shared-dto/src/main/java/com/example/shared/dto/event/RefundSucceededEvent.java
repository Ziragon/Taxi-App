package com.example.shared.dto.event;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;

@Schema(description = "Событие: возврат успешно проведён")
public record RefundSucceededEvent(

        @Schema(description = "ID транзакции возврата", example = "2")
        Long refundTransactionId,

        @Schema(description = "ID оригинальной транзакции", example = "1")
        Long originalTransactionId,

        @Schema(description = "ID поездки", example = "123")
        Long tripId,

        @Schema(description = "ID пассажира", example = "456")
        Long passengerId,

        @Schema(description = "Сумма возврата", example = "25.50")
        BigDecimal amount,

        @Schema(description = "Время успешного возврата")
        Instant succeededAt
) {}

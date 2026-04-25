package com.example.shared.dto.event;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;

@Schema(description = "Событие: платёж успешно проведён")
public record PaymentSucceededEvent(

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

        @Schema(description = "Валюта", example = "USD")
        String currency,

        @Schema(description = "Stripe Payment Intent ID")
        String stripePaymentIntentId,

        @Schema(description = "Время успешного платежа")
        Instant succeededAt
) {}

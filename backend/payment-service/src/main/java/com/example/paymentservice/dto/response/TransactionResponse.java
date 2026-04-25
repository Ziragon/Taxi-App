package com.example.paymentservice.dto.response;

import com.example.paymentservice.entity.Transaction;
import com.example.paymentservice.entity.enums.TransactionStatus;
import com.example.paymentservice.entity.enums.TransactionType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;

@Schema(description = "Транзакция")
public record TransactionResponse(

        @Schema(description = "ID транзакции", example = "1")
        Long id,

        @Schema(description = "ID поездки", example = "123")
        Long tripId,

        @Schema(description = "ID пассажира", example = "456")
        Long passengerId,

        @Schema(description = "ID водителя", example = "789")
        Long driverId,

        @Schema(description = "Тип транзакции", example = "CHARGE")
        TransactionType type,

        @Schema(description = "Сумма", example = "25.50")
        BigDecimal amount,

        @Schema(description = "Валюта", example = "USD")
        String currency,

        @Schema(description = "Статус", example = "SUCCEEDED")
        TransactionStatus status,

        @Schema(description = "Stripe Payment Intent ID")
        String stripePaymentIntentId,

        @Schema(description = "Дата создания")
        Instant createdAt
) {
    public static TransactionResponse from(Transaction transaction) {
        return new TransactionResponse(
                transaction.getId(),
                transaction.getTripId(),
                transaction.getPassengerId(),
                transaction.getDriverId(),
                transaction.getType(),
                transaction.getAmount(),
                transaction.getCurrency(),
                transaction.getStatus(),
                transaction.getStripePaymentIntentId(),
                transaction.getCreatedAt()
        );
    }
}

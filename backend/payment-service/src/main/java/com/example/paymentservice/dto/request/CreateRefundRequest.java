package com.example.paymentservice.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

@Schema(description = "Запрос на возврат средств")
public record CreateRefundRequest(

        @NotNull(message = "ID транзакции обязателен")
        @Schema(description = "ID оригинальной транзакции", example = "1")
        Long transactionId,

        @NotNull(message = "Сумма обязательна")
        @DecimalMin(value = "0.01", message = "Минимальная сумма 0.01")
        @Schema(description = "Сумма возврата", example = "25.50")
        BigDecimal amount,

        @NotBlank(message = "Причина возврата обязательна")
        @Schema(description = "Причина возврата", example = "Trip cancelled")
        String reason
) {}

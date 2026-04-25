package com.example.paymentservice.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

@Schema(description = "Запрос на выплату водителю")
public record CreatePayoutRequest(

        @NotNull(message = "ID поездки обязателен")
        @Schema(description = "ID поездки", example = "123")
        Long tripId,

        @NotNull(message = "ID пассажира обязателен")
        @Schema(description = "ID пассажира", example = "456")
        Long passengerId,

        @NotNull(message = "ID водителя обязателен")
        @Schema(description = "ID водителя", example = "789")
        Long driverId,

        @NotNull(message = "Сумма обязательна")
        @DecimalMin(value = "0.01", message = "Минимальная сумма 0.01")
        @Schema(description = "Сумма выплаты", example = "20.00")
        BigDecimal amount,

        @NotBlank(message = "Валюта обязательна")
        @Size(min = 3, max = 3, message = "Код валюты должен быть 3 символа")
        @Schema(description = "Валюта", example = "usd")
        String currency
) {}

package com.example.paymentservice.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Добавление платёжного метода")
public record AddPaymentMethodRequest(

        @NotBlank(message = "Stripe Payment Method ID обязателен")
        @Schema(description = "ID метода оплаты из Stripe", example = "pm_1234567890")
        String stripePaymentMethodId,

        @Schema(description = "Установить как основной метод", example = "false")
        boolean setAsDefault
) {}

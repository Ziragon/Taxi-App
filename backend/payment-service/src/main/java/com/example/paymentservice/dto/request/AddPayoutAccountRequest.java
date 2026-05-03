package com.example.paymentservice.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Добавление счёта для выплат водителю")
public record AddPayoutAccountRequest(

        @NotBlank(message = "Stripe Account ID обязателен")
        @Schema(description = "ID аккаунта Stripe Connect", example = "acct_1234567890")
        String stripeAccountId,

        @Size(min = 4, max = 4, message = "lastFour должен быть 4 символа")
        String lastFour

) {}

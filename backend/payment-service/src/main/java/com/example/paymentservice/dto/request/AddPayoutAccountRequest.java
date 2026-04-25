package com.example.paymentservice.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Добавление счёта для выплат водителю")
public record AddPayoutAccountRequest(

        @NotBlank(message = "Stripe Account ID обязателен")
        @Schema(description = "ID аккаунта Stripe Connect", example = "acct_1234567890")
        String stripeAccountId,

        @Schema(description = "Последние 4 цифры реквизита", example = "4242")
        String lastFour
) {}

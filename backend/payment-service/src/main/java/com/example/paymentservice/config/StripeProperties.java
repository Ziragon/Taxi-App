package com.example.paymentservice.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "stripe")
public record StripeProperties(

        @NotBlank
        String apiKey,

        @NotBlank
        String webhookSecret
) {
}

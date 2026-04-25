package com.example.paymentservice.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "gateway-auth")
public record GatewayAuthProperties(

        @NotBlank
        String header,

        @NotBlank
        String headerKey
) {
}

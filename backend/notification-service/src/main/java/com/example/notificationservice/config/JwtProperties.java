package com.example.notificationservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;

@Validated
@ConfigurationProperties(prefix = "notification.jwt")
public record JwtProperties(

        @NotBlank
        String secret,

        @NotBlank
        String issuer
) {}

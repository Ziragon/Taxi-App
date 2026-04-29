package com.example.notificationservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;

@Validated
@ConfigurationProperties(prefix = "notification.websocket")
public record WebSocketProperties(

        @NotBlank
        String endpoint,

        @NotBlank
        String allowedOrigins
) {}

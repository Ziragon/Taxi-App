package com.example.notificationservice.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "notification.internal")
public record InternalProperties(

        @NotBlank
        String header,

        @NotBlank
        String headerKey,

        @NotBlank
        String userServiceUrl
) {}

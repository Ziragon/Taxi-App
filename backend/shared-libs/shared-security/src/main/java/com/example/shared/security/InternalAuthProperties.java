package com.example.shared.security;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "internal-auth")
public record InternalAuthProperties(

        @NotBlank
        String header,

        @NotBlank
        String headerKey
) {}
package com.example.shared.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "internal-auth")
public record InternalAuthProperties(

        String header,

        String headerKey
) {}

package com.example.shared.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "internal-auth")
public record InternalAuthProperties(

        String header,

        String headerKey
) {}
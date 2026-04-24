package com.example.shared.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "gateway-auth")
public record GatewayAuthProperties (

    String header,

    String headerKey
) {}

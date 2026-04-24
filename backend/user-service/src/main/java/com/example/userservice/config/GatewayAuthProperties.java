package com.example.userservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "gateway-auth")
public record GatewayAuthProperties (

    String header,

    String headerKey
) {}

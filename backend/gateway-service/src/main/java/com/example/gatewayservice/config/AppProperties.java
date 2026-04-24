package com.example.gatewayservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "gateway")
public record AppProperties (

    String secret,

    String issuer,

    String header,

    String headerKey,

    List<String> openRoutes
) {}

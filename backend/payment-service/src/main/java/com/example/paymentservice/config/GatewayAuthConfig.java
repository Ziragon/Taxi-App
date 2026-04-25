package com.example.paymentservice.config;

import com.example.shared.security.GatewayAuthProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(GatewayAuthProperties.class)
public class GatewayAuthConfig {
}

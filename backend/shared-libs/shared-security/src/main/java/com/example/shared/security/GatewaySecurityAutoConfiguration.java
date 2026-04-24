package com.example.shared.security;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(GatewayAuthProperties.class)
public class GatewaySecurityAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public GatewayAuthFilter gatewayAuthFilter(GatewayAuthProperties properties) {
        return new GatewayAuthFilter(properties);
    }
}
package com.example.shared.security;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({
        GatewayAuthProperties.class,
        InternalAuthProperties.class
})
public class GatewaySecurityAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public GatewayAuthFilter gatewayAuthFilter(GatewayAuthProperties properties) {
        return new GatewayAuthFilter(properties);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(
            prefix = "internal-auth",
            name = {"header", "header-key"},
            matchIfMissing = false
    )
    public InternalAuthFilter internalAuthFilter(InternalAuthProperties properties) {
        return new InternalAuthFilter(properties);
    }
}
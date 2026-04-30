package com.example.shared.security;

import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

@Import(GatewaySecurityAutoConfiguration.class)
public class InternalFeignConfig {

    @Bean
    public RequestInterceptor internalRequestInterceptor(InternalAuthProperties props) {
        return template -> template.header(props.header(), props.headerKey());
    }
}
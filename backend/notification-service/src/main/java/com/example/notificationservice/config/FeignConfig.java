package com.example.notificationservice.config;

import feign.RequestInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class FeignConfig {

    private final InternalProperties internalProperties;

    @Bean
    public RequestInterceptor gatewayHeaderInterceptor() {
        return requestTemplate -> requestTemplate.header(
                internalProperties.gatewayHeader(),
                internalProperties.gatewayHeaderKey()
        );
    }
}

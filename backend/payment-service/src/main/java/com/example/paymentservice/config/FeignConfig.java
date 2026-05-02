package com.example.paymentservice.config;

import feign.Logger;
import feign.RequestInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class FeignConfig {

    @Value("${internal-auth.header}")
    private String internalHeader;

    @Value("${internal-auth.header-key}")
    private String internalHeaderKey;

    @Bean
    public RequestInterceptor internalHeaderInterceptor() {
        return requestTemplate -> requestTemplate.header(internalHeader, internalHeaderKey);
    }

    @Bean
    public Logger.Level feignLoggerLevel() {
        return Logger.Level.FULL;
    }
}

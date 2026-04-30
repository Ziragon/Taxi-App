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
    public RequestInterceptor internalHeaderInterceptor() {
        return requestTemplate -> requestTemplate.header(
                internalProperties.header(),
                internalProperties.headerKey()
        );
    }
}

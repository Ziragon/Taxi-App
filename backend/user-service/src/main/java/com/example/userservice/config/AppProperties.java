package com.example.userservice.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "gateway")
public record AppProperties(

        String secret,

        String issuer,

        @Valid TokenProperties accessToken,
        
        @Valid TokenProperties refreshToken
) {
    public record TokenProperties(@NotNull Duration expiration) {
        public long toMillis() {
            return expiration.toMillis();
        }
    }
}
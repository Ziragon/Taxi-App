package com.example.userservice.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "jwt")
public class AppProperties {

    @NotBlank
    private String secret;

    @NotBlank
    private String issuer;

    @NotNull
    private TokenProperties accessToken;

    @NotNull
    private TokenProperties refreshToken;

    @Getter
    @Setter
    public static class TokenProperties {

        @NotNull
        private Duration expiration;

        public long toMillis() {
            return expiration.toMillis();
        }
    }
}
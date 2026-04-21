package com.example.userservice.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@ConfigurationProperties(prefix = "jwt")
@Validated
public class AppProperties {
    @NotBlank
    private String secret;
    @NotBlank private String issuer;
    private TokenProperties accessToken;
    private TokenProperties refreshToken;


    @NoArgsConstructor
    @AllArgsConstructor
    public static class TokenProperties {
        @NotNull
        private Duration expiration;
        public long toMillis() { return expiration.toMillis(); }
    }
}

package com.example.userservice.config;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Profile;
import org.springframework.validation.annotation.Validated;

@Validated
@Profile("!test")
@ConfigurationProperties(prefix = "admin")
public record AdminProperties(

        @Email
        @NotBlank
        String email,

        @NotBlank
        String phone,

        @NotBlank
        String password
) {}

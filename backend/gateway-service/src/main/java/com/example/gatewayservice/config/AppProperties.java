package com.example.gatewayservice.config;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "gateway")
public class AppProperties {

    @NotBlank
    private String secret;

    @NotBlank
    private String issuer;

    @NotBlank
    private String header;

    @NotBlank
    private String headerKey;

    private List<String> openRoutes;
}
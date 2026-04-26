package com.example.userservice.dto.data;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

public record TokenDto(
        @Schema(description = "Строка токена")
        String token,

        @Schema(description = "Дата истечения", example = "2026-05-20T10:30:00Z")
        Instant expiresAt
) {}

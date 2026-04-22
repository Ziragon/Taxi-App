package com.example.userservice.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Запрос на авторизацию")
public record LoginRequest (

    @NotBlank(message = "Email обязателен")
    @Email(message = "Некорректный формат email")
    @Schema(description = "Email", example = "user@example.com")
    String email,

    @NotBlank(message = "Пароль обязателен")
    @Schema(description = "Пароль", example = "SecurePass123")
    String password
) {}

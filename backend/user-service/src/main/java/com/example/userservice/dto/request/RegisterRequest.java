package com.example.userservice.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Запрос на регистрацию нового пользователя")
public class RegisterRequest {

    @NotBlank(message = "Email обязателен")
    @Email(message = "Некорректный формат email")
    @Schema(description = "Email пользователя", example = "user@example.com")
    private String email;

    @NotBlank(message = "Телефон обязателен")
    @Pattern(regexp = "^\\+?[1-9]\\d{10,14}$", message = "Некорректный формат телефона")
    @Schema(description = "Номер телефона в международном формате", example = "+79991234567")
    private String phone;

    @NotBlank(message = "Пароль обязателен")
    @Size(min = 8, message = "Пароль должен содержать минимум 8 символов")
    @Schema(description = "Пароль (минимум 8 символов)", example = "SecurePass123")
    private String password;
}

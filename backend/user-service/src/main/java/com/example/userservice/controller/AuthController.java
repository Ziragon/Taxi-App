package com.example.userservice.controller;

import com.example.userservice.dto.data.AuthResult;
import com.example.userservice.dto.request.LoginRequest;
import com.example.userservice.dto.request.RefreshTokenRequest;
import com.example.userservice.dto.request.RegisterRequest;
import com.example.userservice.dto.response.AuthResponse;
import com.example.userservice.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Регистрация, авторизация и управление токенами")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @Operation(
            summary = "Регистрация пассажира",
            description = "Создаёт базовый аккаунт. Профиль создаётся отдельно через /api/v1/profiles/{passenger/driver}",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "email": "user@example.com",
                                      "phone": "+79991234567",
                                      "password": "SecurePass123"
                                    }
                                    """)
                    )
            ),
            responses = {
                    @ApiResponse(responseCode = "200", description = "Успешная регистрация",
                            content = @Content(schema = @Schema(implementation = AuthResponse.class))),
                    @ApiResponse(responseCode = "409", description = "Email или телефон уже заняты"),
                    @ApiResponse(responseCode = "422", description = "Ошибка валидации")
            }
    )
    public ResponseEntity<AuthResponse> registerPassenger(
            @Valid @RequestBody RegisterRequest request
    ) {
        AuthResult result = authService.register(
                request.getEmail(),
                request.getPhone(),
                request.getPassword()
        );

        return ResponseEntity.ok(AuthResponse.from(result));
    }

    @PostMapping("/login")
    @Operation(
            summary = "Вход в систему",
            description = "Авторизация по email и паролю. Возвращает access и refresh токены",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "email": "user@example.com",
                                      "password": "SecurePass123"
                                    }
                                    """)
                    )
            ),
            responses = {
                    @ApiResponse(responseCode = "200", description = "Успешная авторизация"),
                    @ApiResponse(responseCode = "401", description = "Неверный email или пароль"),
                    @ApiResponse(responseCode = "403", description = "Аккаунт заблокирован")
            }
    )
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request
    ) {
        AuthResult result = authService.login(request.getEmail(), request.getPassword());

        return ResponseEntity.ok(AuthResponse.from(result));
    }

    @PostMapping("/refresh")
    @Operation(
            summary = "Обновление access токена",
            description = "Принимает refresh токен, валидирует его, отзывает старый и выдаёт новую пару токенов (ротация)",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
                                    }
                                    """)
                    )
            ),
            responses = {
                    @ApiResponse(responseCode = "200", description = "Токены обновлены"),
                    @ApiResponse(responseCode = "401", description = "Недействительный или истёкший refresh токен")
            }
    )
    public ResponseEntity<AuthResponse> refresh(
            @Valid @RequestBody RefreshTokenRequest request
    ) {
        AuthResult result = authService.refreshTokens(request.getRefreshToken());

        return ResponseEntity.ok(AuthResponse.from(result));
    }

    @PostMapping("/logout")
    @Operation(
            summary = "Выход из системы",
            description = "Отзывает все refresh токены пользователя. Access токен продолжит работать до истечения (JWT stateless)",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Успешный выход"),
                    @ApiResponse(responseCode = "401", description = "Не авторизован")
            }
    )
    public ResponseEntity<Void> logout(
            @RequestHeader("X-Account-ID") Long accountId
    ) {
        authService.logout(accountId);

        return ResponseEntity.noContent().build();
    }
}

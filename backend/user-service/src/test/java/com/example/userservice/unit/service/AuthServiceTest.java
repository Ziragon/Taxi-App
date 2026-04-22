package com.example.userservice.unit.service;

import com.example.userservice.dto.data.AuthResult;
import com.example.userservice.dto.data.TokenData;
import com.example.userservice.entity.Account;
import com.example.userservice.entity.enums.AccountRole;
import com.example.userservice.exception.InvalidCredentialsException;
import com.example.userservice.service.AccountService;
import com.example.userservice.service.AuthService;
import com.example.userservice.service.TokenService;
import com.example.userservice.util.JwtUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Unit Tests")
class AuthServiceTest {

    @Mock
    private AccountService accountService;

    @Mock
    private TokenService tokenService;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private AuthService authService;

    @Test
    @DisplayName("Регистрация пассажира: успешное создание аккаунта и выдача токенов")
    void registerPassenger_Success() {
        Instant fixedExpiry = Instant.parse("2026-12-31T23:59:59Z");

        Account account = Account.builder()
                .id(1L)
                .email("passenger@test.com")
                .phone("+79991234567")
                .role(AccountRole.USER)
                .active(true)
                .build();

        when(accountService.createAccount(anyString(), anyString(), anyString()))
                .thenReturn(account);
        when(jwtUtil.generateAccessToken(anyLong(), anyString()))
                .thenReturn(new TokenData("access-token", fixedExpiry));
        when(tokenService.createRefreshToken(account))
                .thenReturn(new TokenData("refresh-token", fixedExpiry));

        AuthResult result = authService.register(
                "passenger@test.com",
                "+79991234567",
                "password123"
        );

        assertThat(result.accessTokenData().token()).isEqualTo("access-token");
        assertThat(result.refreshTokenData().token()).isEqualTo("refresh-token");

        verify(accountService).createAccount("passenger@test.com", "+79991234567", "password123");
        verify(rabbitTemplate).convertAndSend(anyString(), anyString(), any(Map.class));
    }

    @Test
    @DisplayName("Логин: успешная авторизация с валидными данными")
    void login_Success() {
        Instant fixedExpiry = Instant.parse("2026-12-31T23:59:59Z");

        Account account = Account.builder()
                .id(1L)
                .email("user@test.com")
                .passwordHash("hashed-password")
                .role(AccountRole.USER)
                .active(true)
                .build();

        when(accountService.findByEmail("user@test.com")).thenReturn(account);
        when(passwordEncoder.matches("password123", "hashed-password")).thenReturn(true);
        when(jwtUtil.generateAccessToken(anyLong(), anyString()))
                .thenReturn(new TokenData("access-token", fixedExpiry));
        when(tokenService.createRefreshToken(account))
                .thenReturn(new TokenData("refresh-token", fixedExpiry));

        authService.login("user@test.com", "password123");

        verify(passwordEncoder).matches("password123", "hashed-password");
    }

    @Test
    @DisplayName("Логин: неверный пароль -> InvalidCredentialsException")
    void login_InvalidPassword() {
        Account account = Account.builder()
                .id(1L)
                .email("user@test.com")
                .passwordHash("hashed-password")
                .build();

        when(accountService.findByEmail("user@test.com")).thenReturn(account);
        when(passwordEncoder.matches("wrong-password", "hashed-password")).thenReturn(false);

        assertThatThrownBy(() -> authService.login("user@test.com", "wrong-password"))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    @DisplayName("Refresh токен: успешная ротация")
    void refreshToken_Success() {
        Instant fixedExpiry = Instant.parse("2026-12-31T23:59:59Z");

        Account account = Account.builder()
                .id(1L)
                .role(AccountRole.USER)
                .active(true)
                .build();

        when(tokenService.validateAndRotateRefreshToken("old-refresh-token")).thenReturn(account);
        when(jwtUtil.generateAccessToken(anyLong(), anyString()))
                .thenReturn(new TokenData("new-access-token", fixedExpiry));
        when(tokenService.createRefreshToken(account))
                .thenReturn(new TokenData("new-refresh-token", fixedExpiry));

        AuthResult result = authService.refreshTokens("old-refresh-token");

        assertThat(result.accessTokenData().token()).isEqualTo("new-access-token");
        assertThat(result.refreshTokenData().token()).isEqualTo("new-refresh-token");
        verify(tokenService).validateAndRotateRefreshToken("old-refresh-token");
    }
}
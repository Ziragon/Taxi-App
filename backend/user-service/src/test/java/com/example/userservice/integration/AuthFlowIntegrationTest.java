package com.example.userservice.integration;

import com.example.userservice.dto.response.AuthResponse;
import com.example.userservice.entity.Account;
import com.example.userservice.entity.enums.AccountRole;
import com.example.userservice.repository.AccountRepository;
import com.example.userservice.repository.RefreshTokenRepository;
import com.example.userservice.service.AuthService;
import com.example.userservice.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("Auth Flow Integration Tests")
class AuthFlowIntegrationTest extends BaseIntegrationTest{

    @Autowired
    private AuthService authService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @BeforeEach
    void cleanup() {
        refreshTokenRepository.deleteAll();
        accountRepository.deleteAll();
    }

    @Test
    @DisplayName("Полный flow: регистрация -> логин -> refresh -> logout")
    void fullAuthFlow() {
        AuthResponse registerTokens = authService.register(
                "integration@test.com",
                "+79991111111",
                "TestPass123"
        );

        assertThat(registerTokens).satisfies(response -> {
            assertThat(response.accessToken()).isNotBlank();
            assertThat(response.refreshToken()).isNotBlank();
        });
        Long accountId = jwtUtil.extractAccountId(registerTokens.accessToken());

        Account account = accountRepository.findById(accountId).orElseThrow();
        assertThat(account.getEmail()).isEqualTo("integration@test.com");
        assertThat(account.getRole()).isEqualTo(AccountRole.USER);
        assertThat(account.isActive()).isTrue();

        AuthResponse loginTokens = authService.login("integration@test.com", "TestPass123");
        assertThat(loginTokens).satisfies(response -> {
            assertThat(response.accessToken()).isNotBlank();
            assertThat(response.refreshToken()).isNotBlank();
        });

        String oldRefreshToken = loginTokens.refreshToken();
        AuthResponse refreshedTokens = authService.refreshAccessToken(oldRefreshToken);
        assertThat(refreshedTokens.accessToken()).isNotEqualTo(loginTokens.accessToken());

        long activeTokensBeforeLogout = refreshTokenRepository.countByAccountIdAndRevokedFalse(accountId);
        assertThat(activeTokensBeforeLogout).isGreaterThan(0);

        authService.logout(accountId);

        long activeTokensAfterLogout = refreshTokenRepository.countByAccountIdAndRevokedFalse(accountId);
        assertThat(activeTokensAfterLogout).isZero();
    }

    @Test
    @DisplayName("Ротация токенов: старый refresh становится revoked")
    void refreshTokenRotation() {
        AuthResponse tokens = authService.register(
                "driver@test.com",
                "+79992222222",
                "DriverPass456"
        );

        String refreshToken = tokens.refreshToken();
        Long accountId = jwtUtil.extractAccountId(tokens.accessToken());

        authService.refreshAccessToken(refreshToken);

        long activeTokens = refreshTokenRepository.countByAccountIdAndRevokedFalse(accountId);
        assertThat(activeTokens).isEqualTo(1);
    }
}
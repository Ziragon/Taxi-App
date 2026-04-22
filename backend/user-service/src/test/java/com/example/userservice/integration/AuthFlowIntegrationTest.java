package com.example.userservice.integration;

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

import java.util.Map;

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
        Map<String, String> registerTokens = authService.registerPassenger(
                "integration@test.com",
                "+79991111111",
                "TestPass123"
        );

        assertThat(registerTokens).containsKeys("accessToken", "refreshToken");
        Long accountId = jwtUtil.extractAccountId(registerTokens.get("accessToken"));

        Account account = accountRepository.findById(accountId).orElseThrow();
        assertThat(account.getEmail()).isEqualTo("integration@test.com");
        assertThat(account.getRole()).isEqualTo(AccountRole.PASSENGER);
        assertThat(account.getIsActive()).isTrue();

        Map<String, String> loginTokens = authService.login("integration@test.com", "TestPass123");
        assertThat(loginTokens).containsKeys("accessToken", "refreshToken");

        String oldRefreshToken = loginTokens.get("refreshToken");
        Map<String, String> refreshedTokens = authService.refreshAccessToken(oldRefreshToken);
        assertThat(refreshedTokens.get("accessToken")).isNotEqualTo(loginTokens.get("accessToken"));

        long activeTokensBeforeLogout = refreshTokenRepository.countByAccountIdAndRevokedFalse(accountId);
        assertThat(activeTokensBeforeLogout).isGreaterThan(0);

        authService.logout(accountId);

        long activeTokensAfterLogout = refreshTokenRepository.countByAccountIdAndRevokedFalse(accountId);
        assertThat(activeTokensAfterLogout).isZero();
    }

    @Test
    @DisplayName("Ротация токенов: старый refresh становится revoked")
    void refreshTokenRotation() {
        Map<String, String> tokens = authService.registerDriver(
                "driver@test.com",
                "+79992222222",
                "DriverPass456"
        );

        String refreshToken = tokens.get("refreshToken");
        Long accountId = jwtUtil.extractAccountId(tokens.get("accessToken"));

        authService.refreshAccessToken(refreshToken);

        long activeTokens = refreshTokenRepository.countByAccountIdAndRevokedFalse(accountId);
        assertThat(activeTokens).isEqualTo(1);
    }
}
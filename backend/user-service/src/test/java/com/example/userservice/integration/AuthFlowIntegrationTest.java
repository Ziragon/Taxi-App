package com.example.userservice.integration;

import com.example.userservice.dto.data.AuthResult;
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
        AuthResult registerResult = authService.register(
                "integration@test.com",
                "+79991111111",
                "TestPass123"
        );

        assertThat(registerResult).satisfies(result -> {
            assertThat(result.accessTokenData().token()).isNotBlank();
            assertThat(result.refreshTokenData().token()).isNotBlank();
        });
        Long accountId = jwtUtil.extractAccountId(registerResult.accessTokenData().token());

        Account account = accountRepository.findById(accountId).orElseThrow();
        assertThat(account.getEmail()).isEqualTo("integration@test.com");
        assertThat(account.getRole()).isEqualTo(AccountRole.USER);
        assertThat(account.isActive()).isTrue();

        AuthResult loginResult = authService.login("integration@test.com", "TestPass123");
        assertThat(loginResult).satisfies(result -> {
            assertThat(result.accessTokenData().token()).isNotBlank();
            assertThat(result.refreshTokenData().token()).isNotBlank();
        });

        String oldRefreshToken = loginResult.refreshTokenData().token();
        AuthResult refreshedResult = authService.refreshTokens(oldRefreshToken);
        assertThat(refreshedResult.accessTokenData().token()).isNotEqualTo(loginResult.accessTokenData().token());

        long activeTokensBeforeLogout = refreshTokenRepository.countByAccountIdAndRevokedFalse(accountId);
        assertThat(activeTokensBeforeLogout).isGreaterThan(0);

        authService.logout(accountId);

        long activeTokensAfterLogout = refreshTokenRepository.countByAccountIdAndRevokedFalse(accountId);
        assertThat(activeTokensAfterLogout).isZero();
    }

    @Test
    @DisplayName("Ротация токенов: старый refresh становится revoked")
    void refreshTokenRotation() {
        AuthResult result = authService.register(
                "driver@test.com",
                "+79992222222",
                "DriverPass456"
        );

        String refreshToken = result.refreshTokenData().token();
        Long accountId = jwtUtil.extractAccountId(result.accessTokenData().token());

        authService.refreshTokens(refreshToken);

        long activeTokens = refreshTokenRepository.countByAccountIdAndRevokedFalse(accountId);
        assertThat(activeTokens).isEqualTo(1);
    }
}
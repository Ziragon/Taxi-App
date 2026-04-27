package com.example.userservice.integration;

import com.example.userservice.dto.data.AuthDto;
import com.example.userservice.entity.Account;
import com.example.userservice.entity.enums.AccountRole;
import com.example.userservice.repository.AccountRepository;
import com.example.userservice.repository.RefreshTokenRepository;
import com.example.userservice.service.AuthService;
import com.example.userservice.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.concurrent.TimeUnit;

import static com.example.userservice.config.RabbitMQConfig.USER_REGISTERED_QUEUE;
import static org.assertj.core.api.Assertions.*;
import static org.awaitility.Awaitility.await;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@Testcontainers
@DisplayName("Auth Flow Integration Tests")
class AuthFlowIntegrationTest {

    @Container
    static final RabbitMQContainer rabbitMQ = new RabbitMQContainer(
            DockerImageName.parse("rabbitmq:4.0-management-alpine")
    ).withStartupTimeout(java.time.Duration.ofMinutes(2));

    @DynamicPropertySource
    static void rabbitProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.rabbitmq.host", rabbitMQ::getHost);
        registry.add("spring.rabbitmq.port", rabbitMQ::getAmqpPort);
        registry.add("spring.rabbitmq.username", rabbitMQ::getAdminUsername);
        registry.add("spring.rabbitmq.password", rabbitMQ::getAdminPassword);
    }

    @Autowired
    private AuthService authService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @BeforeEach
    void cleanup() {
        refreshTokenRepository.deleteAll();
        accountRepository.deleteAll();
        rabbitTemplate.execute(channel -> {
            channel.queuePurge(USER_REGISTERED_QUEUE);
            return null;
        });
    }

    @Test
    @DisplayName("Полный flow: регистрация -> логин -> refresh -> logout")
    void fullAuthFlow() {
        AuthDto registerResult = authService.register(
                "integration@test.com",
                "+79991111111",
                "TestPass123"
        );

        assertThat(registerResult).satisfies(result -> {
            assertThat(result.accessTokenDto().token()).isNotBlank();
            assertThat(result.refreshTokenDto().token()).isNotBlank();
        });
        Long accountId = jwtUtil.extractAccountId(registerResult.accessTokenDto().token());

        Account account = accountRepository.findById(accountId).orElseThrow();
        assertThat(account.getEmail()).isEqualTo("integration@test.com");
        assertThat(account.getRole()).isEqualTo(AccountRole.USER);
        assertThat(account.isActive()).isTrue();

        AuthDto loginResult = authService.login("integration@test.com", "TestPass123");
        assertThat(loginResult).satisfies(result -> {
            assertThat(result.accessTokenDto().token()).isNotBlank();
            assertThat(result.refreshTokenDto().token()).isNotBlank();
        });

        String oldRefreshToken = loginResult.refreshTokenDto().token();
        AuthDto refreshedResult = authService.refreshTokens(oldRefreshToken);
        assertThat(refreshedResult.accessTokenDto().token()).isNotEqualTo(loginResult.accessTokenDto().token());

        long activeTokensBeforeLogout = refreshTokenRepository.countByAccountIdAndRevokedFalse(accountId);
        assertThat(activeTokensBeforeLogout).isGreaterThan(0);

        authService.logout(accountId);

        long activeTokensAfterLogout = refreshTokenRepository.countByAccountIdAndRevokedFalse(accountId);
        assertThat(activeTokensAfterLogout).isZero();
    }

    @Test
    @DisplayName("Ротация токенов: старый refresh становится revoked")
    void refreshTokenRotation() {
        AuthDto result = authService.register(
                "driver@test.com",
                "+79992222222",
                "DriverPass456"
        );

        String refreshToken = result.refreshTokenDto().token();
        Long accountId = jwtUtil.extractAccountId(result.accessTokenDto().token());

        authService.refreshTokens(refreshToken);

        long activeTokens = refreshTokenRepository.countByAccountIdAndRevokedFalse(accountId);
        assertThat(activeTokens).isEqualTo(1);
    }

    @Test
    @DisplayName("Регистрация: событие user.registered отправлено в RabbitMQ")
    void register_PublishesUserRegisteredEvent() {
        authService.register(
                "rabbit@test.com",
                "+79993333333",
                "RabbitPass123"
        );

        await()
                .atMost(5, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    Message message = rabbitTemplate.receive(USER_REGISTERED_QUEUE, 1000);
                    assertThat(message).isNotNull();

                    String body = new String(message.getBody());
                    assertThat(body).contains("rabbit@test.com");
                    assertThat(body).contains("USER");
                });
    }
}
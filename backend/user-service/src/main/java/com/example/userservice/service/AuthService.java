package com.example.userservice.service;

import com.example.userservice.entity.Account;
import com.example.userservice.entity.enums.AccountRole;
import com.example.userservice.exception.AccountDeactivatedException;
import com.example.userservice.exception.InvalidCredentialsException;
import com.example.userservice.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static com.example.userservice.config.RabbitMQConfig.USER_EVENTS_EXCHANGE;
import static com.example.userservice.config.RabbitMQConfig.USER_REGISTERED_ROUTING_KEY;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AccountService accountService;
    private final TokenService tokenService;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;
    private final RabbitTemplate rabbitTemplate;

    @Transactional
    public Map<String, String> registerPassenger(String email, String phone, String password) {
        Account account = accountService.createAccount(email, phone, password, AccountRole.PASSENGER);

        publishUserRegisteredEvent(account.getId(), email, AccountRole.PASSENGER);

        return generateTokens(account);
    }

    @Transactional
    public Map<String, String> registerDriver(String email, String phone, String password) {
        Account account = accountService.createAccount(email, phone, password, AccountRole.DRIVER);

        publishUserRegisteredEvent(account.getId(), email, AccountRole.DRIVER);

        return generateTokens(account);
    }

    @Transactional(readOnly = true)
    public Map<String, String> login(String email, String password) {
        Account account = accountService.findByEmail(email);

        if (!passwordEncoder.matches(password, account.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        if (!account.getIsActive()) {
            throw new AccountDeactivatedException(account.getId());
        }

        return generateTokens(account);
    }

    @Transactional
    public Map<String, String> refreshAccessToken(String refreshToken) {
        Long accountId = tokenService.validateAndRotateRefreshToken(refreshToken);
        Account account = accountService.findById(accountId);

        if (!account.getIsActive()) {
            throw new AccountDeactivatedException(accountId);
        }

        return generateTokens(account);
    }

    @Transactional
    public void logout(Long accountId) {
        tokenService.revokeAllTokens(accountId);
    }

    private Map<String, String> generateTokens(Account account) {
        String accessToken = jwtUtil.generateAccessToken(account.getId(), account.getRole().name());
        String refreshToken = tokenService.createRefreshToken(account.getId());

        return Map.of(
                "accessToken", accessToken,
                "refreshToken", refreshToken
        );
    }

    private void publishUserRegisteredEvent(Long accountId, String email, AccountRole role) {
        Map<String, Object> event = Map.of(
                "accountId", accountId,
                "email", email,
                "role", role.name(),
                "timestamp", System.currentTimeMillis()
        );

        rabbitTemplate.convertAndSend(USER_EVENTS_EXCHANGE, USER_REGISTERED_ROUTING_KEY, event);
    }
}
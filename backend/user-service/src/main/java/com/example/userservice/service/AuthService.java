package com.example.userservice.service;

import com.example.userservice.dto.data.*;
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

import java.time.Instant;
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
    private final DriverProfileService driverProfileService;

    @Transactional
    public AuthDto register(String email, String phone, String password) {

        Account account = accountService.createAccount(email, phone, password);

        publishUserRegisteredEvent(account.getId(), email);

        return generateTokens(account);
    }

    @Transactional
    public AuthDto login(String email, String password) {

        Account account = accountService.findByEmail(email);

        if (!account.isActive()) {
            throw new AccountDeactivatedException(account.getId());
        }

        if (!passwordEncoder.matches(password, account.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        return generateTokens(account);
    }

    @Transactional
    public AuthDto refreshTokens(String refreshToken) {
        Account account = tokenService.validateAndRotateRefreshToken(refreshToken);

        if (!account.isActive()) {
            throw new AccountDeactivatedException(account.getId());
        }

        return generateTokens(account);
    }

    @Transactional
    public void logout(Long accountId) {
        tokenService.revokeAllTokens(accountId);
    }

    private AuthDto generateTokens(Account account) {
        TokenDto accessTokenDto = jwtUtil.generateAccessToken(account.getId(), account.getRole().name());
        TokenDto refreshTokenDto = tokenService.createRefreshToken(account);

        return new AuthDto(
                AccountDto.from(account),
                accessTokenDto,
                refreshTokenDto,
                account.getPassengerProfile() != null
                        ? PassengerProfileDto.from(account.getPassengerProfile())
                        : null,
                account.getDriverProfile() != null
                        ? DriverProfileDto.from(account.getDriverProfile())
                        : null
        );
    }

    private void publishUserRegisteredEvent(Long accountId, String email) {
        Map<String, Object> event = Map.of(
                "accountId", accountId,
                "email", email,
                "role", AccountRole.USER,
                "timestamp", Instant.now()
        );

        rabbitTemplate.convertAndSend(USER_EVENTS_EXCHANGE, USER_REGISTERED_ROUTING_KEY, event);
    }
}
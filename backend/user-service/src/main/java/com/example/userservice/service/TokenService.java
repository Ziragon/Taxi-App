package com.example.userservice.service;

import com.example.userservice.config.AppProperties;
import com.example.userservice.entity.RefreshToken;
import com.example.userservice.exception.TokenExpiredException;
import com.example.userservice.repository.RefreshTokenRepository;
import com.example.userservice.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class TokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtUtil jwtUtil;
    private final AppProperties appProperties;
    private final PasswordEncoder passwordEncoder;
    private final AccountService accountService;

    @Transactional
    public String createRefreshToken(Long accountId) {
        String token = jwtUtil.generateRefreshToken(accountId);
        String tokenHash = passwordEncoder.encode(token);

        RefreshToken refreshToken = RefreshToken.builder()
                .account(accountService.findById(accountId))
                .tokenHash(tokenHash)
                .expiresAt(Instant.now().plusMillis(appProperties.getRefreshToken().toMillis()))
                .revoked(false)
                .build();

        refreshTokenRepository.save(refreshToken);
        return token;
    }

    @Transactional
    public Long validateAndRotateRefreshToken(String token) {
        Long accountId = jwtUtil.extractAccountId(token);

        if (jwtUtil.isTokenExpired(token)) {
            throw new TokenExpiredException("Refresh");
        }

        RefreshToken storedToken = refreshTokenRepository.findByTokenHashAndRevokedFalse(
                        passwordEncoder.encode(token))
                .orElseThrow(() -> new TokenExpiredException("Refresh"));

        if (storedToken.getExpiresAt().isBefore(Instant.now())) {
            throw new TokenExpiredException("Refresh");
        }

        storedToken.setRevoked(true);
        refreshTokenRepository.save(storedToken);

        return accountId;
    }

    @Transactional
    public void revokeAllTokens(Long accountId) {
        refreshTokenRepository.revokeAllByAccountId(accountId);
    }

    @Transactional
    public void cleanupExpiredTokens() {
        refreshTokenRepository.deleteAllExpired(Instant.now());
    }
}
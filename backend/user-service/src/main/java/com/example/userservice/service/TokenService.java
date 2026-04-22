package com.example.userservice.service;


import com.example.userservice.config.AppProperties;
import com.example.userservice.entity.RefreshToken;
import com.example.userservice.exception.TokenExpiredException;
import com.example.userservice.repository.RefreshTokenRepository;
import com.example.userservice.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class TokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtUtil jwtUtil;
    private final AppProperties appProperties;
    private final AccountService accountService;

    @Transactional
    public String createRefreshToken(Long accountId) {
        String rawToken = jwtUtil.generateRefreshToken(accountId);
        String tokenHash = hashToken(rawToken);

        RefreshToken refreshToken = RefreshToken.builder()
                .account(accountService.findById(accountId))
                .tokenHash(tokenHash)
                .expiresAt(Instant.now().plusMillis(appProperties.getRefreshToken().toMillis()))
                .revoked(false)
                .build();

        refreshTokenRepository.save(refreshToken);
        return rawToken;
    }

    @Transactional
    public Long validateAndRotateRefreshToken(String rawToken) {
        if (jwtUtil.isTokenExpired(rawToken)) {
            throw new TokenExpiredException("Refresh");
        }

        Long accountId = jwtUtil.extractAccountId(rawToken);

        String tokenHash = hashToken(rawToken);

        RefreshToken storedToken = refreshTokenRepository
                .findByTokenHashAndRevokedFalse(tokenHash)
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

    private String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }
}
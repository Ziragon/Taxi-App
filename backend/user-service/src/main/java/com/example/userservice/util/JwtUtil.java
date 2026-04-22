package com.example.userservice.util;

import com.example.userservice.config.AppProperties;
import com.example.userservice.dto.data.TokenData;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class JwtUtil {

    private final AppProperties appProperties;

    public TokenData generateAccessToken(Long accountId, String role) {
        Instant expiresAt = Instant.now().plusMillis(appProperties.getAccessToken().toMillis());
        String token = generateToken(accountId, role, appProperties.getAccessToken().toMillis());
        return new TokenData(token, expiresAt);
    }

    public TokenData generateRefreshToken() {
        Instant expiresAt = Instant.now().plusMillis(appProperties.getRefreshToken().toMillis());
        return new TokenData(UUID.randomUUID().toString(), expiresAt);
    }

    private String generateToken(Long accountId, String role, long expirationMs) {
        Instant now = Instant.now();
        Instant expiration = now.plusMillis(expirationMs);

        var builder = Jwts.builder()
                .subject(accountId.toString())
                .issuer(appProperties.getIssuer())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiration))
                .signWith(getSecretKey());

        if (role != null) {
            builder.claim("role", role);
        }

        builder.id(UUID.randomUUID().toString());

        return builder.compact();
    }

    public Claims parseToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(getSecretKey())
                    .requireIssuer(appProperties.getIssuer())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException e) {
            throw new IllegalArgumentException("Invalid JWT token", e);
        }
    }

    public Long extractAccountId(String token) {
        return Long.parseLong(parseToken(token).getSubject());
    }

    public boolean isTokenExpired(String token) {
        return parseToken(token).getExpiration().before(new Date());
    }

    private SecretKey getSecretKey() {
        return Keys.hmacShaKeyFor(appProperties.getSecret().getBytes(StandardCharsets.UTF_8));
    }
}
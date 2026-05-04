package com.example.notificationservice.websocket;

import com.example.notificationservice.security.JwtUtil;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtHandshakeInterceptor implements HandshakeInterceptor {

    private static final String TOKEN_PARAM = "token";
    public static final String SESSION_ATTR_USER_ID = "userId";
    public static final String SESSION_ATTR_USER_ROLE = "userRole";
    public static final String SESSION_ATTR_USER_TYPE = "userType";

    private final JwtUtil jwtUtil;

    @Override
    public boolean beforeHandshake(
            @NonNull ServerHttpRequest request,
            @NonNull ServerHttpResponse response,
            @NonNull WebSocketHandler wsHandler,
            @NonNull Map<String, Object> attributes
    ) {
        if (!(request instanceof ServletServerHttpRequest servletRequest)) {
            log.warn("WS Handshake rejected: not a servlet request");
            return false;
        }

        String token = servletRequest.getServletRequest().getParameter(TOKEN_PARAM);
        if (token == null || token.isBlank()) {
            log.warn("WS Handshake rejected: missing token param");
            return false;
        }

        Claims claims = jwtUtil.parseTokenSilently(token);
        if (claims == null) {
            log.warn("WS Handshake rejected: invalid token");
            return false;
        }

        Long userId = Long.parseLong(claims.getSubject());
        String role = claims.get("role", String.class);

        attributes.put(SESSION_ATTR_USER_ID, userId);
        attributes.put(SESSION_ATTR_USER_ROLE, role);

        log.debug("WS Handshake accepted: userId={}, role={}", userId, role);
        return true;
    }

    @Override
    public void afterHandshake(
            @NonNull ServerHttpRequest request,
            @NonNull ServerHttpResponse response,
            @NonNull WebSocketHandler wsHandler,
            Exception exception
    ) {

    }
}

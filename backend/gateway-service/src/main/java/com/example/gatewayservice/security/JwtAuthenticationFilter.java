package com.example.gatewayservice.security;

import com.example.gatewayservice.config.AppProperties;
import com.example.gatewayservice.util.JwtUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.PathContainer;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.pattern.PathPatternParser;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    private final JwtUtil jwtUtil;
    private final AppProperties appProperties;
    private final PathPatternParser parser = new PathPatternParser();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        if (isOpenPath(path)) {
            log.info("Anonymous access to open path: {}", path);
            return chain.filter(mutateRequest(exchange, null, null, true));
        }

        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return onError(exchange, path, "Token validation failed");
        }

        String token = authHeader.substring(7);

        try {
            Claims claims = jwtUtil.parseToken(token);
            String accountId = claims.getSubject();
            String role = claims.get("role", String.class);

            if (accountId == null || role == null) {
                return onError(exchange, path, "Invalid token claims");
            }

            return chain.filter(mutateRequest(exchange, accountId, role, false));

        } catch (Exception e) {
            log.error("JWT validation failed: {}", e.getMessage());
            return onError(exchange, path, "Token validation failed");
        }
    }

    private ServerWebExchange mutateRequest(ServerWebExchange exchange, String userId, String role, boolean isAnonymous) {
        return exchange.mutate()
                .request(exchange.getRequest().mutate()
                        .headers(h -> {
                            h.remove("X-User-Id");
                            h.remove("X-User-Role");
                            h.remove("X-User-Anonymous");
                            h.remove(appProperties.getHeader());
                            h.remove(HttpHeaders.AUTHORIZATION);

                            if (!isAnonymous) {
                                h.add("X-User-Id", userId);
                                h.add("X-User-Role", role);
                            }
                            h.add("X-User-Anonymous", String.valueOf(isAnonymous));
                            h.add(appProperties.getHeader(), appProperties.getHeaderKey());
                        })
                        .build())
                .build();
    }

    private Mono<Void> onError(ServerWebExchange exchange, String path, String err) {
        String requestId = exchange.getRequest().getId();
        log.warn("Auth error in request with id {}: {} at path {}", requestId, err, path);
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);

        return exchange.getResponse().setComplete();
    }

    private boolean isOpenPath(String path) {
        return appProperties.getOpenRoutes().stream()
                .anyMatch(pattern -> parser.parse(pattern).matches(PathContainer.parsePath(path)));
    }

    @Override
    public int getOrder() {
        return -1;
    }
}

package com.example.notificationservice.websocket;

import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    @Override
    public Message<?> preSend(@NonNull Message<?> message, @NonNull MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(
                message, StompHeaderAccessor.class
        );

        if (accessor == null) {
            return message;
        }

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
            if (sessionAttributes == null) {
                log.warn("STOMP CONNECT: no session attributes, rejecting");
                throw new IllegalStateException("Unauthorized WebSocket connection");
            }

            Long userId = (Long) sessionAttributes.get(
                    JwtHandshakeInterceptor.SESSION_ATTR_USER_ID
            );
            String role = (String) sessionAttributes.get(
                    JwtHandshakeInterceptor.SESSION_ATTR_USER_ROLE
            );

            if (userId == null) {
                log.warn("STOMP CONNECT: userId not found in session");
                throw new IllegalStateException("Unauthorized WebSocket connection");
            }

            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                    userId.toString(),
                    null,
                    role != null
                            ? List.of(new SimpleGrantedAuthority(role))
                            : List.of()
            );

            accessor.setUser(auth);
            log.debug("STOMP CONNECT authenticated: userId={}, role={}", userId, role);
        }

        return message;
    }
}

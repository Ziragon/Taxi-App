package com.example.notificationservice.websocket;

import com.example.notificationservice.client.UserServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class StompSessionEventListener {

    private final WebSocketSessionRegistry sessionRegistry;
    private final UserServiceClient userServiceClient;

    @EventListener
    public void onConnected(SessionConnectedEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());

        Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
        if (sessionAttributes == null) {
            log.warn("SessionConnectedEvent: sessionAttributes is null, sessionId={}",
                    accessor.getSessionId());
            return;
        }

        Long userId = (Long) sessionAttributes.get(JwtHandshakeInterceptor.SESSION_ATTR_USER_ID);
        String userType = (String) sessionAttributes.get(JwtHandshakeInterceptor.SESSION_ATTR_USER_TYPE);
        String sessionId = accessor.getSessionId();

        sessionRegistry.register(sessionId, userId, userType);

        log.info("STOMP connected: sessionId={}, userId={}, userType={}",
                sessionId, userId, userType);
    }

    @EventListener
    public void onSubscribe(SessionSubscribeEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        Long userId = (Long) accessor.getSessionAttributes().get(
                JwtHandshakeInterceptor.SESSION_ATTR_USER_ID
        );
        log.debug("STOMP subscribed: sessionId={}, userId={}, destination={}",
                accessor.getSessionId(), userId, accessor.getDestination());
    }

    @EventListener
    public void onDisconnect(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();

        Long userId = sessionRegistry.getUserId(sessionId);
        boolean isDriver = sessionRegistry.isDriver(sessionId);

        log.info("STOMP disconnected: sessionId={}, userId={}, isDriver={}, closeStatus={}",
                sessionId, userId, isDriver, event.getCloseStatus());

        if (isDriver && userId != null) {
            try {
                userServiceClient.setDriverOffline(userId);
                log.info("Driver {} set OFFLINE after WS disconnect", userId);
            } catch (Exception e) {
                log.error("Failed to set driver {} OFFLINE after disconnect: {}",
                        userId, e.getMessage());
            }
        }

        sessionRegistry.unregister(sessionId);
    }
}
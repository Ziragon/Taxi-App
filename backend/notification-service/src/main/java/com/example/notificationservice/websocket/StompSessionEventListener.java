package com.example.notificationservice.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

@Slf4j
@Component
public class StompSessionEventListener {

    @EventListener
    public void onConnected(SessionConnectedEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        Long userId = (Long) accessor.getSessionAttributes().get(
                JwtHandshakeInterceptor.SESSION_ATTR_USER_ID
        );
        log.info("STOMP connected: sessionId={}, userId={}",
                accessor.getSessionId(), userId);
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
        Long userId = (Long) accessor.getSessionAttributes().get(
                JwtHandshakeInterceptor.SESSION_ATTR_USER_ID
        );
        log.info("STOMP disconnected: sessionId={}, userId={}, closeStatus={}",
                accessor.getSessionId(), userId, event.getCloseStatus());
    }
}

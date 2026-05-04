package com.example.notificationservice.websocket;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class WebSocketSessionRegistry {

    private final Map<String, Long> sessionUserMap = new ConcurrentHashMap<>();
    private final Map<String, String> sessionUserTypeMap = new ConcurrentHashMap<>();

    public void register(String sessionId, Long userId, String userType) {
        sessionUserMap.put(sessionId, userId);
        if (userType != null) {
            sessionUserTypeMap.put(sessionId, userType);
        }
    }

    public void unregister(String sessionId) {
        sessionUserMap.remove(sessionId);
        sessionUserTypeMap.remove(sessionId);
    }

    public Long getUserId(String sessionId) {
        return sessionUserMap.get(sessionId);
    }

    public boolean isDriver(String sessionId) {
        return "DRIVER".equals(sessionUserTypeMap.get(sessionId));
    }
}

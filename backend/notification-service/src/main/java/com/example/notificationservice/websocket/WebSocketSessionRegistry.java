package com.example.notificationservice.websocket;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class WebSocketSessionRegistry {

    private final Map<String, Long> sessionUserMap = new ConcurrentHashMap<>();

    private final Map<String, String> sessionRoleMap = new ConcurrentHashMap<>();

    public void register(String sessionId, Long userId, String role) {
        sessionUserMap.put(sessionId, userId);
        if (role != null) {
            sessionRoleMap.put(sessionId, role);
        }
    }

    public void unregister(String sessionId) {
        sessionUserMap.remove(sessionId);
        sessionRoleMap.remove(sessionId);
    }

    public Long getUserId(String sessionId) {
        return sessionUserMap.get(sessionId);
    }

    public String getRole(String sessionId) {
        return sessionRoleMap.get(sessionId);
    }

    public boolean isDriver(String sessionId) {
        return "ROLE_DRIVER".equals(sessionRoleMap.get(sessionId));
    }
}

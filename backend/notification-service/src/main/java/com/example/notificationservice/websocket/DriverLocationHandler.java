package com.example.notificationservice.websocket;

import com.example.notificationservice.client.UserServiceClient;
import com.example.notificationservice.dto.DriverLocationRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

@Slf4j
@Controller
@RequiredArgsConstructor
public class DriverLocationHandler {

    private final UserServiceClient userServiceClient;

    @MessageMapping("/driver/location")
    public void handleLocationUpdate(
            @Payload DriverLocationRequest request,
            SimpMessageHeaderAccessor headerAccessor
    ) {
        Long driverId = (Long) headerAccessor.getSessionAttributes()
                .get(JwtHandshakeInterceptor.SESSION_ATTR_USER_ID);

        if (driverId == null) {
            log.warn("Location update rejected: no userId in session");
            return;
        }

        log.debug("Location update: driverId={}, lng={}, lat={}, vehicleClass={}",
                driverId,
                request.longitude(),
                request.latitude(),
                request.vehicleClass()
        );

        try {
            userServiceClient.updateDriverLocation(driverId, request);
            log.debug("Location updated successfully: driverId={}", driverId);
        } catch (Exception e) {
            log.error("Failed to update location: driverId={}, error={}",
                    driverId, e.getMessage(), e);
        }
    }
}
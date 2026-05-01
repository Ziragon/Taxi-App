package com.example.tripservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DriverResponsePublisher {

    private final StringRedisTemplate stringRedisTemplate;
    private static final String DRIVER_RESPONSE_CHANNEL = "driver:response:";

    // Publish-метод для ответа водителя
    public void publish(Long tripId, String action, Long driverId) {
        String payload = action + ":" + tripId + ":" + driverId;
        stringRedisTemplate.convertAndSend(DRIVER_RESPONSE_CHANNEL + tripId, payload);
    }
}

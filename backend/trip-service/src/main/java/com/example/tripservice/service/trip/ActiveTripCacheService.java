package com.example.tripservice.service.trip;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

// Кэширует статус активной поездки водителя
@Component
@RequiredArgsConstructor
@Slf4j
public class ActiveTripCacheService {

    private final RedisTemplate<String, Long> longRedisTemplate;

    private static final String DRIVER_ACTIVE_TRIP_KEY = "driver:%d:active_trip";

    private static final Duration TTL = Duration.ofHours(6);

    public void save(Long driverId, Long tripId) {
        longRedisTemplate.opsForValue().set(
                DRIVER_ACTIVE_TRIP_KEY.formatted(driverId),
                tripId,
                TTL
        );
    }

    public boolean hasActiveTrip(Long driverId) {
        return longRedisTemplate.hasKey(DRIVER_ACTIVE_TRIP_KEY.formatted(driverId));
    }

    public Long getActiveTripId(Long driverId) {
        return longRedisTemplate.opsForValue().get(DRIVER_ACTIVE_TRIP_KEY.formatted(driverId));
    }

    // Вызывается при COMPLETED / CANCELLED
    public void remove(Long driverId) {
        longRedisTemplate.delete(DRIVER_ACTIVE_TRIP_KEY.formatted(driverId));
    }
}
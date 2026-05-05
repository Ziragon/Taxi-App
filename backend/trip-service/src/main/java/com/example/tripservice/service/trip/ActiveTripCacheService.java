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
    private static final String PASSENGER_ACTIVE_TRIP_KEY = "passenger:%d:active_trip";

    private static final Duration TTL = Duration.ofHours(2);

    public void saveForDriver(Long driverId, Long tripId) {
        longRedisTemplate.opsForValue().set(
                DRIVER_ACTIVE_TRIP_KEY.formatted(driverId),
                tripId,
                TTL
        );
    }

    public void saveForPassenger(Long passengerId, Long tripId) {
        longRedisTemplate.opsForValue().set(
                PASSENGER_ACTIVE_TRIP_KEY.formatted(passengerId),
                tripId,
                TTL
        );
    }

    public boolean driverHasActiveTrip(Long driverId) {
        return longRedisTemplate.hasKey(DRIVER_ACTIVE_TRIP_KEY.formatted(driverId));
    }

    public Long getPassengerActiveTripId(Long passengerId) {
        return longRedisTemplate.opsForValue().get(
                PASSENGER_ACTIVE_TRIP_KEY.formatted(passengerId)
        );
    }

    // Вызывается при COMPLETED / CANCELLED
    public void removeForDriver(Long driverId) {
        longRedisTemplate.delete(DRIVER_ACTIVE_TRIP_KEY.formatted(driverId));
    }

    public void removeForPassenger(Long passengerId) {
        longRedisTemplate.delete(PASSENGER_ACTIVE_TRIP_KEY.formatted(passengerId));
    }
}
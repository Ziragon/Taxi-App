package com.example.userservice.service;

import com.example.userservice.dto.data.DriverLocationDto;
import com.example.userservice.entity.enums.DriverStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class DriverCachingService {

    private static final String KEY_LOCATION_PREFIX = "driver:location:";
    private static final String KEY_STATUS_PREFIX = "driver:status:";
    private static final String ONLINE_DRIVERS_KEY = "drivers:online";

    private final RedisTemplate<String, DriverLocationDto> redisLocationTemplate;
    private final RedisTemplate<String, String> redisStatusTemplate;

    @Value("${app.driver.location.ttl-seconds}")
    private long ttlSeconds;

    public void updateLocation(Long driverId, DriverLocationDto dto) {
        String key = KEY_LOCATION_PREFIX + driverId;
        redisLocationTemplate.opsForValue().set(key, dto, ttlSeconds, TimeUnit.SECONDS);
    }

    public void updateStatus(Long driverId, DriverStatus status) {
        String key = KEY_STATUS_PREFIX + driverId;
        redisStatusTemplate.opsForValue().set(key, status.name(), ttlSeconds, TimeUnit.SECONDS);

        if (status == DriverStatus.ONLINE) {
            redisStatusTemplate.opsForSet().add(ONLINE_DRIVERS_KEY, String.valueOf(driverId));
        } else {
            redisStatusTemplate.opsForSet().remove(ONLINE_DRIVERS_KEY, String.valueOf(driverId));
        }
    }

    public void deleteDriver(Long driverId) {
        redisLocationTemplate.delete(KEY_LOCATION_PREFIX + driverId);
        redisStatusTemplate.delete(KEY_STATUS_PREFIX + driverId);
        redisStatusTemplate.opsForSet().remove(ONLINE_DRIVERS_KEY, String.valueOf(driverId));
    }

    public List<DriverLocationDto> getOnlineDriverLocations() {
        Set<String> onlineIds = redisStatusTemplate.opsForSet().members(ONLINE_DRIVERS_KEY);

        if (onlineIds == null || onlineIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> locationKeys = onlineIds.stream()
                .map(id -> KEY_LOCATION_PREFIX + id)
                .toList();

        List<DriverLocationDto> locations = redisLocationTemplate.opsForValue()
                .multiGet(locationKeys);

        if (locations == null) {
            return Collections.emptyList();
        }

        return locations.stream()
                .filter(Objects::nonNull)
                .toList();
    }
}

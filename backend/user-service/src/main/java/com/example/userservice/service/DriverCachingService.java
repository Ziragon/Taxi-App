package com.example.userservice.service;

import com.example.userservice.dto.data.DriverLocationDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class DriverCachingService {

    private static final String KEY_PREFIX = "driver:location:";
    private final RedisTemplate<String, DriverLocationDto> redisTemplate;

    @Value("${app.driver.location.ttl-seconds}")
    private long ttlSeconds;

    public void updateLocation(DriverLocationDto dto) {
        String key = KEY_PREFIX + dto.driverId();
        redisTemplate.opsForValue().set(key, dto, ttlSeconds, TimeUnit.SECONDS);
    }
}

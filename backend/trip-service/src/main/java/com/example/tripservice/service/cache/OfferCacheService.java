package com.example.tripservice.service.cache;

import com.example.shared.exception.common.AccessDeniedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
@Slf4j
public class OfferCacheService {

    private final RedisTemplate<String, Long> longRedisTemplate;
    private static final String DRIVER_BUSY_KEY = "driver_busy:";

    @Value("${searching.duration}")
    private int searchDuration;

    private static final String ACTIVE_OFFER_KEY = "active_offer:";

    public void setActiveOffer(Long tripId, Long driverId) {
        longRedisTemplate.opsForValue().set(
                ACTIVE_OFFER_KEY + tripId,
                driverId,
                Duration.ofSeconds(searchDuration + 5L)
        );
    }

    public Long getActiveOffer(Long tripId) {
        return longRedisTemplate.opsForValue().get(ACTIVE_OFFER_KEY + tripId);
    }

    public void removeActiveOffer(Long tripId) {
        longRedisTemplate.delete(ACTIVE_OFFER_KEY + tripId);
    }

    public void validateAndRemoveActiveOffer(Long tripId, Long driverId) {
        String key = ACTIVE_OFFER_KEY + tripId;
        Long expected = longRedisTemplate.opsForValue().getAndDelete(key);

        if (expected == null || !expected.equals(driverId)) {
            log.warn("Driver {} tried to respond to trip {} but offer was sent to driver {}",
                    driverId, tripId, expected);
            throw new AccessDeniedException();
        }
    }

    // Блокировка водителя на время предложения о заказе
    public boolean tryLockDriver(Long driverId, Long tripId) {
        Boolean locked = longRedisTemplate.opsForValue().setIfAbsent(
                DRIVER_BUSY_KEY + driverId,
                tripId,
                Duration.ofSeconds(searchDuration + 5L)
        );
        return Boolean.TRUE.equals(locked);
    }

    public void unlockDriver(Long driverId) {
        longRedisTemplate.delete(DRIVER_BUSY_KEY + driverId);
    }
}

package com.example.tripservice.service;

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

    public void validateActiveOffer(Long tripId, Long driverId) {
        Long expected = getActiveOffer(tripId);
        if (expected == null || !expected.equals(driverId)) {
            log.warn("Driver {} tried to respond to trip {} but offer was sent to driver {}",
                    driverId, tripId, expected);
            throw new AccessDeniedException();
        }
    }
}

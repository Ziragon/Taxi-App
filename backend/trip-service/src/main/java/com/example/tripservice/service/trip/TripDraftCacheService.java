package com.example.tripservice.service.trip;

import com.example.tripservice.dto.data.TripDraftDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class TripDraftCacheService {

    private final RedisTemplate<String, TripDraftDto> draftRedisTemplate;

    private static final String DRAFT_KEY = "passenger:%d:trip_draft";
    private static final Duration TTL = Duration.ofMinutes(10);

    public void save(Long passengerId, TripDraftDto draft) {
        draftRedisTemplate.opsForValue().set(
                DRAFT_KEY.formatted(passengerId),
                draft,
                TTL
        );
        log.debug("Saved trip draft for passenger {}", passengerId);
    }

    public Optional<TripDraftDto> get(Long passengerId) {
        TripDraftDto draft = draftRedisTemplate.opsForValue()
                .get(DRAFT_KEY.formatted(passengerId));
        return Optional.ofNullable(draft);
    }

    public void delete(Long passengerId) {
        draftRedisTemplate.delete(DRAFT_KEY.formatted(passengerId));
        log.debug("Deleted trip draft for passenger {}", passengerId);
    }
}
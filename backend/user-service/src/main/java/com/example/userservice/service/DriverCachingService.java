package com.example.userservice.service;

import com.example.userservice.dto.data.DriverLocationDto;
import com.example.userservice.entity.enums.DriverStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.geo.*;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.domain.geo.GeoReference;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class DriverCachingService {

    private static final String KEY_LOCATION_PREFIX = "driver:location:";
    private static final String KEY_STATUS_PREFIX = "driver:status:";
    private static final String ONLINE_DRIVERS_KEY = "drivers:online";
    private static final String GEO_KEY = "drivers:geo";

    private final RedisTemplate<String, DriverLocationDto> redisLocationTemplate;
    private final RedisTemplate<String, String> redisStatusTemplate;

    @Value("${app.driver.location.ttl-seconds}")
    private long ttlSeconds;

    public void updateLocation(Long driverId, DriverLocationDto dto) {
        String key = KEY_LOCATION_PREFIX + driverId;
        redisLocationTemplate.opsForValue().set(key, dto, ttlSeconds, TimeUnit.SECONDS);

        redisStatusTemplate.opsForGeo().add(
                GEO_KEY,
                new Point(dto.longitude().doubleValue(), dto.latitude().doubleValue()),
                String.valueOf(driverId)
        );
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
        redisStatusTemplate.opsForGeo().remove(GEO_KEY, String.valueOf(driverId));
    }

    // Определение ближайших водителей через Redis GEO
    public List<DriverLocationDto> getNearbyOnlineDrivers(double lng, double lat, double radiusKm) {

        GeoReference<String> center = GeoReference.fromCoordinate(new Point(lng, lat));

        Distance radius = new Distance(radiusKm, Metrics.KILOMETERS);

        // Параметры поиска по гео
        RedisGeoCommands.GeoSearchCommandArgs args = RedisGeoCommands.GeoSearchCommandArgs
                .newGeoSearchArgs()
                .includeCoordinates()
                .sortAscending()
                .limit(50);

        // Выдает result по вхождению в радиус окружности с центром - координаты пользователя
        GeoResults<RedisGeoCommands.GeoLocation<String>> geoResults =
                redisStatusTemplate.opsForGeo()
                        .search(GEO_KEY, center, radius, args);

        if (geoResults == null) return Collections.emptyList();

        List<Long> nearbyIds = geoResults.getContent().stream()
                .map(r -> Long.parseLong(r.getContent().getName()))
                .toList();

        if (nearbyIds.isEmpty()) return Collections.emptyList();

        List<Long> onlineNearbyIds = nearbyIds.stream()
                .filter(id -> DriverStatus.ONLINE.name().equals(
                        redisStatusTemplate.opsForValue().get(KEY_STATUS_PREFIX + id)))
                .toList();

        if (onlineNearbyIds.isEmpty()) return Collections.emptyList();

        List<String> locationKeys = onlineNearbyIds.stream()
                .map(id -> KEY_LOCATION_PREFIX + id)
                .toList();

        List<DriverLocationDto> locations =
                redisLocationTemplate.opsForValue().multiGet(locationKeys);

        return locations == null ? Collections.emptyList() :
                locations.stream().filter(Objects::nonNull).toList();
    }
}

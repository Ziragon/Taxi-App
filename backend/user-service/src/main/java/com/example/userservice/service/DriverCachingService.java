package com.example.userservice.service;

import com.example.shared.dto.enums.VehicleClass;
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
import java.util.Set;
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
    private final RedisTemplate<String, String> redisStringTemplate;

    @Value("${app.driver.location.ttl-seconds}")
    private long ttlSeconds;

    public void updateLocation(DriverLocationDto dto) {
        String key = KEY_LOCATION_PREFIX + dto.driverId();
        redisLocationTemplate.opsForValue().set(key, dto, ttlSeconds, TimeUnit.SECONDS);

        redisStringTemplate.opsForGeo().add(
                GEO_KEY,
                new Point(
                        dto.location().longitude().doubleValue(),
                        dto.location().latitude().doubleValue()
                ),
                String.valueOf(dto.driverId())
        );
    }

    public void updateStatus(Long driverId, DriverStatus status) {
        String key = KEY_STATUS_PREFIX + driverId;
        redisStringTemplate.opsForValue().set(key, status.name(), ttlSeconds, TimeUnit.SECONDS);

        if (status == DriverStatus.ONLINE) {
            redisStringTemplate.opsForSet().add(ONLINE_DRIVERS_KEY, String.valueOf(driverId));
        } else {
            redisStringTemplate.opsForSet().remove(ONLINE_DRIVERS_KEY, String.valueOf(driverId));
        }
    }

    public void deleteDriver(Long driverId) {
        redisLocationTemplate.delete(KEY_LOCATION_PREFIX + driverId);
        redisStringTemplate.delete(KEY_STATUS_PREFIX + driverId);
        redisStringTemplate.opsForSet().remove(ONLINE_DRIVERS_KEY, String.valueOf(driverId));
        redisStringTemplate.opsForGeo().remove(GEO_KEY, String.valueOf(driverId));
    }

    public List<DriverLocationDto> getNearbyOnlineDrivers(
            double lng, double lat, double radiusKm, VehicleClass vehicleClass) {

        GeoReference<String> center = GeoReference.fromCoordinate(new Point(lng, lat));
        Distance radius = new Distance(radiusKm, Metrics.KILOMETERS);

        RedisGeoCommands.GeoSearchCommandArgs args = RedisGeoCommands.GeoSearchCommandArgs
                .newGeoSearchArgs()
                .sortAscending()
                .limit(50);

        GeoResults<RedisGeoCommands.GeoLocation<String>> geoResults =
                redisStringTemplate.opsForGeo()
                        .search(GEO_KEY, center, radius, args);

        if (geoResults == null) return Collections.emptyList();

        List<String> locationKeys = geoResults.getContent().stream()
                .map(r -> KEY_LOCATION_PREFIX + r.getContent().getName())
                .toList();

        if (locationKeys.isEmpty()) return Collections.emptyList();

        List<DriverLocationDto> locations =
                redisLocationTemplate.opsForValue().multiGet(locationKeys);

        if (locations == null) return Collections.emptyList();

        return locations.stream()
                .filter(Objects::nonNull)
                .filter(dto -> DriverStatus.ONLINE.name().equals(
                        redisStringTemplate.opsForValue().get(KEY_STATUS_PREFIX + dto.driverId())))
                .filter(dto -> vehicleClass == null || vehicleClass.equals(dto.vehicleClass()))
                .toList();
    }

    public List<DriverLocationDto> getNearbyOnlineDrivers(
            double lng, double lat, double radiusKm) {
        return getNearbyOnlineDrivers(lng, lat, radiusKm, null);
    }

    public Set<String> getOnlineDriverIds() {
        return redisStringTemplate.opsForSet().members(ONLINE_DRIVERS_KEY);
    }

    public List<DriverLocationDto> multiGetLocations(List<String> keys) {
        List<DriverLocationDto> result = redisLocationTemplate.opsForValue().multiGet(keys);
        return result == null ? Collections.emptyList() : result;
    }
}

package com.example.userservice.service;

import com.example.shared.dto.enums.VehicleClass;
import com.example.shared.dto.data.DriverLocationDto;
import com.example.shared.dto.enums.DriverStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.geo.*;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.domain.geo.GeoReference;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class DriverCachingService {

    private static final String KEY_LOCATION_PREFIX = "driver:location:";
    private static final String KEY_STATUS_PREFIX = "driver:status:";
    private static final String GEO_KEY = "drivers:geo";
    private static final String ONLINE_DRIVERS_ZSET_KEY = "drivers:online:heartbeat";

    private static final long HEARTBEAT_TIMEOUT_SECONDS = 60;

    private final RedisTemplate<String, DriverLocationDto> redisLocationTemplate;
    private final RedisTemplate<String, String> redisStringTemplate;

    @Value("${app.driver.location.ttl-seconds}")
    private long ttlSeconds;

    public void updateLocation(DriverLocationDto dto) {
        String statusKey = KEY_STATUS_PREFIX + dto.driverId();
        String currentStatus = redisStringTemplate.opsForValue().get(statusKey);

        if (currentStatus == null ||
                currentStatus.equals(DriverStatus.OFFLINE.name())) {
            return;
        }

        String locationKey = KEY_LOCATION_PREFIX + dto.driverId();
        redisLocationTemplate.opsForValue().set(locationKey, dto, ttlSeconds, TimeUnit.SECONDS);

        if (DriverStatus.ONLINE.name().equals(currentStatus)) {
            redisStringTemplate.expire(statusKey, ttlSeconds, TimeUnit.SECONDS);
        }

        refreshHeartbeat(dto.driverId());

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

        if (status == DriverStatus.BUSY) {
            redisStringTemplate.opsForValue().set(key, status.name());
        } else {
            redisStringTemplate.opsForValue().set(key, status.name(), ttlSeconds, TimeUnit.SECONDS);
        }

        if (status == DriverStatus.ONLINE) {
            refreshHeartbeat(driverId);
        } else if (status == DriverStatus.OFFLINE) {
            redisStringTemplate.opsForZSet().remove(ONLINE_DRIVERS_ZSET_KEY, String.valueOf(driverId));
        }
    }

    @Scheduled(fixedDelay = 30_000)
    public void evictStaleDrivers() {
        double cutoff = System.currentTimeMillis() / 1000.0 - HEARTBEAT_TIMEOUT_SECONDS;

        Set<String> stale = redisStringTemplate.opsForZSet()
                .rangeByScore(ONLINE_DRIVERS_ZSET_KEY, 0, cutoff);

        if (stale == null || stale.isEmpty()) return;

        for (String driverIdStr : stale) {
            Long driverId = Long.parseLong(driverIdStr);
            log.warn("Evicting stale driver: {}", driverId);
            deleteDriver(driverId); // чистит geo, location, status
        }
    }

    public void deleteDriver(Long driverId) {
        redisLocationTemplate.delete(KEY_LOCATION_PREFIX + driverId);
        redisStringTemplate.delete(KEY_STATUS_PREFIX + driverId);
        redisStringTemplate.opsForZSet().remove(ONLINE_DRIVERS_ZSET_KEY, String.valueOf(driverId));
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

        List<String> statusKeys = locations.stream()
                .filter(Objects::nonNull)
                .map(dto -> KEY_STATUS_PREFIX + dto.driverId())
                .toList();

        List<String> statuses = redisStringTemplate.opsForValue().multiGet(statusKeys);

        List<DriverLocationDto> filtered = new ArrayList<>();
        List<DriverLocationDto> nonNull = locations.stream()
                .filter(Objects::nonNull).toList();

        for (int i = 0; i < nonNull.size(); i++) {
            DriverLocationDto dto = nonNull.get(i);
            String status = statuses != null ? statuses.get(i) : null;
            if (DriverStatus.ONLINE.name().equals(status)
                    && (vehicleClass == null || vehicleClass.equals(dto.vehicleClass()))) {
                filtered.add(dto);
            }
        }

        return filtered;
    }

    public DriverStatus getStatus(Long driverId) {
        String key = KEY_STATUS_PREFIX + driverId;
        String statusName = redisStringTemplate.opsForValue().get(key);

        if (statusName == null) {
            return DriverStatus.OFFLINE;
        }

        return DriverStatus.valueOf(statusName);
    }

    public List<DriverLocationDto> getNearbyOnlineDrivers(
            double lng, double lat, double radiusKm) {
        return getNearbyOnlineDrivers(lng, lat, radiusKm, null);
    }

    public Set<String> getOnlineDriverIds() {
        Set<String> result = redisStringTemplate.opsForZSet()
                .range(ONLINE_DRIVERS_ZSET_KEY, 0, -1);
        return result != null ? result : Collections.emptySet();
    }

    public List<DriverLocationDto> multiGetLocations(List<String> keys) {
        List<DriverLocationDto> result = redisLocationTemplate.opsForValue().multiGet(keys);
        return result == null ? Collections.emptyList() : result;
    }

    private void refreshHeartbeat(Long driverId) {
        double score = System.currentTimeMillis() / 1000.0;
        redisStringTemplate.opsForZSet()
                .add(ONLINE_DRIVERS_ZSET_KEY, String.valueOf(driverId), score);
    }
}

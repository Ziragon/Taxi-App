package com.example.userservice.integration;

import com.example.userservice.config.TestContainersConfig;
import com.example.userservice.dto.data.DriverLocationDto;
import com.example.userservice.dto.data.LocationDto;
import com.example.userservice.entity.enums.DriverStatus;
import com.example.userservice.entity.enums.VehicleClass;
import com.example.userservice.service.DriverCachingService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestContainersConfig.class)
@Transactional
@DisplayName("Driver Caching Integration Tests")
class DriverCacheIntegrationTest {

    private static final String KEY_LOCATION_PREFIX = "driver:location:";
    private static final String KEY_STATUS_PREFIX   = "driver:status:";
    private static final String ONLINE_DRIVERS_KEY  = "drivers:online";
    private static final String GEO_KEY             = "drivers:geo";

    private static final Long DRIVER_ID_1 = 1L;
    private static final Long DRIVER_ID_2 = 2L;
    private static final Long DRIVER_ID_3 = 3L;

    // Координаты точки
    private static final double CENTER_LNG = 37.61;
    private static final double CENTER_LAT = 55.75;

    // Координаты в радиусе 5 км
    private static final String NEAR_LNG_1 = "37.62";   // ~0.8 км от центра
    private static final String NEAR_LAT_1 = "55.76";

    private static final String NEAR_LNG_2 = "37.63";   // ~2.1 км от центра
    private static final String NEAR_LAT_2 = "55.77";

    // Координаты за пределами 5 км (~85 км)
    private static final String FAR_LNG_3  = "38.50";
    private static final String FAR_LAT_3  = "56.30";

    private static final double SEARCH_RADIUS_KM = 5.0;

    @Autowired
    private DriverCachingService driverCachingService;

    @Autowired
    private RedisTemplate<String, DriverLocationDto> redisLocationTemplate;

    @Autowired
    private RedisTemplate<String, String> redisStatusTemplate;

    @AfterEach
    void cleanUp() {
        redisLocationTemplate.delete(KEY_LOCATION_PREFIX + DRIVER_ID_1);
        redisLocationTemplate.delete(KEY_LOCATION_PREFIX + DRIVER_ID_2);
        redisLocationTemplate.delete(KEY_LOCATION_PREFIX + DRIVER_ID_3);

        redisStatusTemplate.delete(KEY_STATUS_PREFIX + DRIVER_ID_1);
        redisStatusTemplate.delete(KEY_STATUS_PREFIX + DRIVER_ID_2);
        redisStatusTemplate.delete(KEY_STATUS_PREFIX + DRIVER_ID_3);

        redisStatusTemplate.delete(ONLINE_DRIVERS_KEY);
        redisStatusTemplate.delete(GEO_KEY);
    }

    private DriverLocationDto buildDriverLocation(Long driverId, String lng, String lat) {
        return new DriverLocationDto(
                driverId,
                new LocationDto(new BigDecimal(lng), new BigDecimal(lat)),
                null
        );
    }

    private DriverLocationDto buildDriverLocation(Long driverId, String lng, String lat, VehicleClass vehicleClass) {
        return new DriverLocationDto(
                driverId,
                new LocationDto(new BigDecimal(lng), new BigDecimal(lat)),
                vehicleClass
        );
    }

    @Nested
    @DisplayName("updateLocation()")
    class UpdateLocation {

        @Test
        @DisplayName("Сохраняет локацию по корректному ключу")
        void shouldSaveLocationByCorrectKey() {
            driverCachingService.updateLocation(buildDriverLocation(DRIVER_ID_1, "37.61", "55.75"));

            DriverLocationDto stored = redisLocationTemplate
                    .opsForValue()
                    .get(KEY_LOCATION_PREFIX + DRIVER_ID_1);

            assertThat(stored).isNotNull();
            assertThat(stored.location().latitude()).isEqualByComparingTo("55.75");
            assertThat(stored.location().longitude()).isEqualByComparingTo("37.61");
        }

        @Test
        @DisplayName("Установлен положительный TTL")
        void shouldSetPositiveTtl() {
            driverCachingService.updateLocation(buildDriverLocation(DRIVER_ID_1, "37.61", "55.75"));

            Long ttl = redisLocationTemplate.getExpire(KEY_LOCATION_PREFIX + DRIVER_ID_1);
            assertThat(ttl).isNotNull().isPositive();
        }

        @Test
        @DisplayName("Добавляет водителя в GEO-индекс")
        void shouldAddDriverToGeoIndex() {
            driverCachingService.updateLocation(buildDriverLocation(DRIVER_ID_1, "37.61", "55.75"));

            List<Point> positions = redisStatusTemplate.opsForGeo()
                    .position(GEO_KEY, String.valueOf(DRIVER_ID_1));

            assertThat(positions).isNotNull().isNotEmpty();
            assertThat(positions.getFirst()).isNotNull();
        }
    }

    @Nested
    @DisplayName("updateStatus()")
    class UpdateStatus {

        @Test
        @DisplayName("Сохраняет статус по корректному ключу")
        void shouldSaveStatusByCorrectKey() {
            driverCachingService.updateStatus(DRIVER_ID_1, DriverStatus.ONLINE);

            String storedStatus = redisStatusTemplate
                    .opsForValue()
                    .get(KEY_STATUS_PREFIX + DRIVER_ID_1);

            assertThat(storedStatus).isEqualTo(DriverStatus.ONLINE.name());
        }

        @Test
        @DisplayName("При статусе ONLINE водитель добавляется в drivers:online")
        void shouldAddDriverToOnlineSetWhenStatusIsOnline() {
            driverCachingService.updateStatus(DRIVER_ID_1, DriverStatus.ONLINE);

            Set<String> onlineIds = redisStatusTemplate.opsForSet().members(ONLINE_DRIVERS_KEY);
            assertThat(onlineIds)
                    .isNotNull()
                    .isNotEmpty()
                    .contains(String.valueOf(DRIVER_ID_1));
        }

        @Test
        @DisplayName("При статусе OFFLINE водитель удаляется из drivers:online")
        void shouldRemoveDriverFromOnlineSetWhenStatusIsOffline() {
            driverCachingService.updateStatus(DRIVER_ID_1, DriverStatus.ONLINE);
            driverCachingService.updateStatus(DRIVER_ID_2, DriverStatus.ONLINE);

            Set<String> onlineIdsBefore = redisStatusTemplate.opsForSet().members(ONLINE_DRIVERS_KEY);
            assertThat(onlineIdsBefore)
                    .isNotNull()
                    .isNotEmpty()
                    .contains(String.valueOf(DRIVER_ID_1), String.valueOf(DRIVER_ID_2));

            driverCachingService.updateStatus(DRIVER_ID_1, DriverStatus.OFFLINE);

            Set<String> onlineIdsAfter = redisStatusTemplate.opsForSet().members(ONLINE_DRIVERS_KEY);
            assertThat(onlineIdsAfter)
                    .isNotNull()
                    .isNotEmpty()
                    .doesNotContain(String.valueOf(DRIVER_ID_1))
                    .contains(String.valueOf(DRIVER_ID_2));
        }

        @Test
        @DisplayName("При статусе BUSY водитель удаляется из drivers:online")
        void shouldRemoveDriverFromOnlineSetWhenStatusIsBusy() {
            driverCachingService.updateStatus(DRIVER_ID_1, DriverStatus.ONLINE);
            driverCachingService.updateStatus(DRIVER_ID_2, DriverStatus.ONLINE);

            Set<String> onlineIdsBefore = redisStatusTemplate.opsForSet().members(ONLINE_DRIVERS_KEY);
            assertThat(onlineIdsBefore)
                    .isNotNull()
                    .isNotEmpty()
                    .contains(String.valueOf(DRIVER_ID_1), String.valueOf(DRIVER_ID_2));

            driverCachingService.updateStatus(DRIVER_ID_1, DriverStatus.BUSY);

            Set<String> onlineIdsAfter = redisStatusTemplate.opsForSet().members(ONLINE_DRIVERS_KEY);
            assertThat(onlineIdsAfter)
                    .isNotNull()
                    .isNotEmpty()
                    .doesNotContain(String.valueOf(DRIVER_ID_1))
                    .contains(String.valueOf(DRIVER_ID_2));
        }
    }

    @Nested
    @DisplayName("deleteDriver()")
    class DeleteDriver {

        @Test
        @DisplayName("Удаляет локацию из Redis")
        void shouldDeleteLocation() {
            driverCachingService.updateLocation(buildDriverLocation(DRIVER_ID_1, NEAR_LNG_1, NEAR_LAT_1));

            driverCachingService.deleteDriver(DRIVER_ID_1);

            DriverLocationDto stored = redisLocationTemplate
                    .opsForValue()
                    .get(KEY_LOCATION_PREFIX + DRIVER_ID_1);
            assertThat(stored).isNull();
        }

        @Test
        @DisplayName("Удаляет статус из Redis")
        void shouldDeleteStatus() {
            driverCachingService.updateStatus(DRIVER_ID_1, DriverStatus.ONLINE);

            driverCachingService.deleteDriver(DRIVER_ID_1);

            String storedStatus = redisStatusTemplate
                    .opsForValue()
                    .get(KEY_STATUS_PREFIX + DRIVER_ID_1);
            assertThat(storedStatus).isNull();
        }

        @Test
        @DisplayName("Удаляет водителя из сета drivers:online")
        void shouldRemoveDriverFromOnlineSet() {
            driverCachingService.updateStatus(DRIVER_ID_1, DriverStatus.ONLINE);
            driverCachingService.updateStatus(DRIVER_ID_2, DriverStatus.ONLINE);

            Set<String> onlineIdsBefore = redisStatusTemplate.opsForSet().members(ONLINE_DRIVERS_KEY);
            assertThat(onlineIdsBefore)
                    .isNotNull()
                    .isNotEmpty()
                    .contains(String.valueOf(DRIVER_ID_1), String.valueOf(DRIVER_ID_2));

            driverCachingService.deleteDriver(DRIVER_ID_1);

            Set<String> onlineIdsAfter = redisStatusTemplate.opsForSet().members(ONLINE_DRIVERS_KEY);
            assertThat(onlineIdsAfter)
                    .isNotNull()
                    .isNotEmpty()
                    .doesNotContain(String.valueOf(DRIVER_ID_1))
                    .contains(String.valueOf(DRIVER_ID_2));
        }

        @Test
        @DisplayName("Удаляет водителя из GEO-индекса")
        void shouldRemoveDriverFromGeoIndex() {
            driverCachingService.updateLocation(buildDriverLocation(DRIVER_ID_1, NEAR_LNG_1, NEAR_LAT_1));

            List<Point> positionsBefore = redisStatusTemplate.opsForGeo()
                    .position(GEO_KEY, String.valueOf(DRIVER_ID_1));
            assertThat(positionsBefore).isNotNull().isNotEmpty();
            assertThat(positionsBefore.getFirst()).isNotNull();

            driverCachingService.deleteDriver(DRIVER_ID_1);

            List<Point> positionsAfter = redisStatusTemplate.opsForGeo()
                    .position(GEO_KEY, String.valueOf(DRIVER_ID_1));
            assertThat(positionsAfter).satisfiesAnyOf(
                    list -> assertThat(list).isNullOrEmpty(),
                    list -> assertThat(list.getFirst()).isNull()
            );
        }
    }

    @Nested
    @DisplayName("getNearbyOnlineDrivers()")
    class GetNearbyOnlineDrivers {

        @Test
        @DisplayName("Возвращает пустой список, если нет водителей в радиусе")
        void shouldReturnEmptyListWhenNoDriversInRadius() {
            List<DriverLocationDto> result = driverCachingService
                    .getNearbyOnlineDrivers(CENTER_LNG, CENTER_LAT, SEARCH_RADIUS_KM);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Возвращает локации онлайн-водителей в радиусе")
        void shouldReturnOnlineDriversWithinRadius() {
            driverCachingService.updateStatus(DRIVER_ID_1, DriverStatus.ONLINE);
            driverCachingService.updateStatus(DRIVER_ID_2, DriverStatus.ONLINE);
            driverCachingService.updateLocation(buildDriverLocation(DRIVER_ID_1, NEAR_LNG_1, NEAR_LAT_1));
            driverCachingService.updateLocation(buildDriverLocation(DRIVER_ID_2, NEAR_LNG_2, NEAR_LAT_2));

            List<DriverLocationDto> result = driverCachingService
                    .getNearbyOnlineDrivers(CENTER_LNG, CENTER_LAT, SEARCH_RADIUS_KM);

            assertThat(result).hasSize(2);
            assertThat(result)
                    .extracting(dto -> dto.location().longitude())
                    .map(BigDecimal::doubleValue)
                    .containsExactlyInAnyOrder(
                            Double.parseDouble(NEAR_LNG_1),
                            Double.parseDouble(NEAR_LNG_2)
                    );
        }

        @Test
        @DisplayName("Не включает водителей за пределами радиуса")
        void shouldExcludeDriversOutsideRadius() {
            driverCachingService.updateStatus(DRIVER_ID_1, DriverStatus.ONLINE);
            driverCachingService.updateStatus(DRIVER_ID_3, DriverStatus.ONLINE);
            driverCachingService.updateLocation(buildDriverLocation(DRIVER_ID_1, NEAR_LNG_1, NEAR_LAT_1));
            driverCachingService.updateLocation(buildDriverLocation(DRIVER_ID_3, FAR_LNG_3, FAR_LAT_3));

            List<DriverLocationDto> result = driverCachingService
                    .getNearbyOnlineDrivers(CENTER_LNG, CENTER_LAT, SEARCH_RADIUS_KM);

            assertThat(result).hasSize(1);
            assertThat(result.getFirst().location().longitude()).isEqualByComparingTo(NEAR_LNG_1);
            assertThat(result.getFirst().location().latitude()).isEqualByComparingTo(NEAR_LAT_1);
        }

        @Test
        @DisplayName("Не включает OFFLINE/BUSY водителей, даже если они в радиусе")
        void shouldExcludeOfflineAndBusyDriversWithinRadius() {
            driverCachingService.updateStatus(DRIVER_ID_1, DriverStatus.ONLINE);
            driverCachingService.updateStatus(DRIVER_ID_2, DriverStatus.OFFLINE);
            driverCachingService.updateStatus(DRIVER_ID_3, DriverStatus.BUSY);
            driverCachingService.updateLocation(buildDriverLocation(DRIVER_ID_1, NEAR_LNG_1, NEAR_LAT_1));
            driverCachingService.updateLocation(buildDriverLocation(DRIVER_ID_2, NEAR_LNG_2, NEAR_LAT_2));
            driverCachingService.updateLocation(buildDriverLocation(DRIVER_ID_3, "37.615", "55.755"));

            List<DriverLocationDto> result = driverCachingService
                    .getNearbyOnlineDrivers(CENTER_LNG, CENTER_LAT, SEARCH_RADIUS_KM);

            assertThat(result).hasSize(1);
            assertThat(result.getFirst().location().longitude()).isEqualByComparingTo(NEAR_LNG_1);
        }

        @Test
        @DisplayName("Фильтрует null: водитель в GEO и онлайн, но локация не задана")
        void shouldFilterNullLocations() {
            driverCachingService.updateStatus(DRIVER_ID_1, DriverStatus.ONLINE);
            driverCachingService.updateStatus(DRIVER_ID_2, DriverStatus.ONLINE);
            driverCachingService.updateLocation(buildDriverLocation(DRIVER_ID_2, NEAR_LNG_2, NEAR_LAT_2));

            redisStatusTemplate.opsForGeo().add(
                    GEO_KEY,
                    new Point(Double.parseDouble(NEAR_LNG_1), Double.parseDouble(NEAR_LAT_1)),
                    String.valueOf(DRIVER_ID_1)
            );

            List<DriverLocationDto> result = driverCachingService
                    .getNearbyOnlineDrivers(CENTER_LNG, CENTER_LAT, SEARCH_RADIUS_KM);

            assertThat(result).hasSize(1);
            assertThat(result.getFirst().location().longitude()).isEqualByComparingTo(NEAR_LNG_2);
            assertThat(result.getFirst().location().latitude()).isEqualByComparingTo(NEAR_LAT_2);
        }

        @Test
        @DisplayName("Фильтрует водителей по VehicleClass")
        void shouldFilterByVehicleClass() {
            driverCachingService.updateStatus(DRIVER_ID_1, DriverStatus.ONLINE);
            driverCachingService.updateStatus(DRIVER_ID_2, DriverStatus.ONLINE);
            driverCachingService.updateLocation(
                    buildDriverLocation(DRIVER_ID_1, NEAR_LNG_1, NEAR_LAT_1, VehicleClass.ECONOMY));
            driverCachingService.updateLocation(
                    buildDriverLocation(DRIVER_ID_2, NEAR_LNG_2, NEAR_LAT_2, VehicleClass.BUSINESS));

            List<DriverLocationDto> result = driverCachingService
                    .getNearbyOnlineDrivers(CENTER_LNG, CENTER_LAT, SEARCH_RADIUS_KM, VehicleClass.ECONOMY);

            assertThat(result).hasSize(1);
            assertThat(result.getFirst().vehicleClass()).isEqualTo(VehicleClass.ECONOMY);
            assertThat(result.getFirst().location().longitude()).isEqualByComparingTo(NEAR_LNG_1);
        }
    }
}
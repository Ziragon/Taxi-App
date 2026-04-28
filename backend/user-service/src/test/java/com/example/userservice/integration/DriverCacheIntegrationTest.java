package com.example.userservice.integration;

import com.example.userservice.config.TestContainersConfig;
import com.example.userservice.dto.data.DriverLocationDto;
import com.example.userservice.entity.enums.DriverStatus;
import com.example.userservice.service.DriverCachingService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestContainersConfig.class)
@Transactional
@DisplayName("Driver Caching Integration Tests")
class DriverCacheIntegrationTest {

    private static final String KEY_LOCATION_PREFIX = "driver:location:";
    private static final String KEY_STATUS_PREFIX   = "driver:status:";
    private static final String ONLINE_DRIVERS_KEY  = "drivers:online";

    private static final Long DRIVER_ID_1 = 1L;
    private static final Long DRIVER_ID_2 = 2L;
    private static final Long DRIVER_ID_3 = 3L;

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
    }

    private DriverLocationDto buildLocation(String lon, String lat) {
        return new DriverLocationDto(
                new BigDecimal(lon),
                new BigDecimal(lat)
        );
    }

    @Nested
    @DisplayName("updateLocation()")
    class UpdateLocation {

        @Test
        @DisplayName("Сохраняет локацию по корректному ключу")
        void shouldSaveLocationByCorrectKey() {

            DriverLocationDto dto = buildLocation("37.61", "55.75");

            driverCachingService.updateLocation(DRIVER_ID_1, dto);

            DriverLocationDto stored = redisLocationTemplate
                    .opsForValue()
                    .get(KEY_LOCATION_PREFIX + DRIVER_ID_1);

            assertThat(stored).isNotNull();
            assertThat(stored.latitude()).isEqualTo("55.75");
            assertThat(stored.longitude()).isEqualTo("37.61");
        }

        @Test
        @DisplayName("Установлен положительный TTL")
        void shouldSetPositiveTtl() {

            DriverLocationDto dto = buildLocation("55.75", "37.61");

            driverCachingService.updateLocation(DRIVER_ID_1, dto);

            Long ttl = redisLocationTemplate.getExpire(KEY_LOCATION_PREFIX + DRIVER_ID_1);
            assertThat(ttl).isNotNull().isPositive();
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
            assertThat(onlineIds).contains(String.valueOf(DRIVER_ID_1));
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

            driverCachingService.updateLocation(DRIVER_ID_1, buildLocation("55.0", "37.0"));

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
                    .contains(String.valueOf(DRIVER_ID_1));

            driverCachingService.deleteDriver(DRIVER_ID_1);

            Set<String> onlineIdsAfter = redisStatusTemplate.opsForSet().members(ONLINE_DRIVERS_KEY);
            assertThat(onlineIdsAfter)
                    .isNotNull()
                    .isNotEmpty()
                    .doesNotContain(String.valueOf(DRIVER_ID_1))
                    .contains(String.valueOf(DRIVER_ID_2));
        }
    }

    @Nested
    @DisplayName("getOnlineDriverLocations()")
    class GetOnlineDriverLocations {

        @Test
        @DisplayName("Возвращает пустой список, если онлайн-водителей нет")
        void shouldReturnEmptyListWhenNoOnlineDrivers() {

            List<DriverLocationDto> result = driverCachingService.getOnlineDriverLocations();

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Возвращает локации всех онлайн-водителей")
        void shouldReturnLocationsOfAllOnlineDrivers() {
            // given
            driverCachingService.updateStatus(DRIVER_ID_1, DriverStatus.ONLINE);
            driverCachingService.updateStatus(DRIVER_ID_2, DriverStatus.ONLINE);
            driverCachingService.updateLocation(DRIVER_ID_1, buildLocation("37.1", "55.1"));
            driverCachingService.updateLocation(DRIVER_ID_2, buildLocation("37.2", "55.2"));

            List<DriverLocationDto> result = driverCachingService.getOnlineDriverLocations();

            assertThat(result).hasSize(2);
            assertThat(result)
                    .extracting(DriverLocationDto::latitude, DriverLocationDto::longitude)
                    .containsExactlyInAnyOrder(
                            tuple(new BigDecimal("55.1"), new BigDecimal("37.1")),
                            tuple(new BigDecimal("55.2"), new BigDecimal("37.2"))
                    );
        }

        @Test
        @DisplayName("Возвращает только локации онлайн-водителей, офлайн не включаются")
        void shouldReturnOnlyOnlineDriverLocations() {

            driverCachingService.updateStatus(DRIVER_ID_1, DriverStatus.ONLINE);
            driverCachingService.updateStatus(DRIVER_ID_2, DriverStatus.OFFLINE);
            driverCachingService.updateLocation(DRIVER_ID_1, buildLocation("37.1", "55.1"));
            driverCachingService.updateLocation(DRIVER_ID_2, buildLocation("37.2", "55.2"));

            List<DriverLocationDto> result = driverCachingService.getOnlineDriverLocations();

            assertThat(result).hasSize(1);
            assertThat(result.getFirst().latitude()).isEqualTo("55.1");
            assertThat(result.getFirst().longitude()).isEqualTo("37.1");
        }

        @Test
        @DisplayName("Фильтрует null: водитель в сете online, но локация не задана")
        void shouldFilterNullLocations() {

            driverCachingService.updateStatus(DRIVER_ID_1, DriverStatus.ONLINE);
            driverCachingService.updateStatus(DRIVER_ID_2, DriverStatus.ONLINE);
            driverCachingService.updateLocation(DRIVER_ID_2, buildLocation("37.2", "55.2"));

            List<DriverLocationDto> result = driverCachingService.getOnlineDriverLocations();

            assertThat(result).hasSize(1);
            assertThat(result.getFirst().latitude()).isEqualTo("55.2");
            assertThat(result.getFirst().longitude()).isEqualTo("37.2");
        }
    }
}
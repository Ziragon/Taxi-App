package com.example.userservice.integration;

import com.example.shared.dto.enums.VehicleClass;
import com.example.userservice.config.TestContainersConfig;
import com.example.shared.dto.data.DriverLocationDto;
import com.example.userservice.dto.data.LocationDto;
import com.example.userservice.entity.Account;
import com.example.userservice.entity.DriverLocation;
import com.example.userservice.entity.DriverProfile;
import com.example.userservice.entity.enums.AccountRole;
import com.example.userservice.entity.enums.DriverStatus;
import com.example.userservice.repository.AccountRepository;
import com.example.userservice.repository.DriverLocationRepository;
import com.example.userservice.repository.DriverProfileRepository;
import com.example.userservice.service.DriverCachingService;
import com.example.userservice.service.DriverLocationService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestContainersConfig.class)
@DisplayName("Driver Location Integration Tests")
class DriverLocationIntegrationTest {

    private static final String KEY_LOCATION_PREFIX = "driver:location:";
    private static final String KEY_STATUS_PREFIX   = "driver:status:";
    private static final String ONLINE_DRIVERS_KEY  = "drivers:online";
    private static final String GEO_KEY             = "drivers:geo";

    private static final VehicleClass ECONOMY = VehicleClass.ECONOMY;
    private static final VehicleClass COMFORT  = VehicleClass.COMFORT;

    private static final BigDecimal CENTER_LNG    = new BigDecimal("37.61");
    private static final BigDecimal CENTER_LAT    = new BigDecimal("55.75");
    private static final BigDecimal SEARCH_RADIUS = new BigDecimal("5.0");

    private Long driverId1;
    private Long driverId2;
    private Long driverId3;

    @Autowired
    private DriverLocationService driverLocationService;

    @Autowired
    private DriverCachingService driverCachingService;

    @Autowired
    private DriverLocationRepository driverLocationRepository;

    @Autowired
    private DriverProfileRepository driverProfileRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private RedisTemplate<String, DriverLocationDto> redisLocationTemplate;

    @Autowired
    private RedisTemplate<String, String> redisStatusTemplate;

    @BeforeEach
    void setUpDatabase() {
        Account account1 = accountRepository.save(buildAccount());
        Account account2 = accountRepository.save(buildAccount());
        Account account3 = accountRepository.save(buildAccount());

        driverProfileRepository.save(buildDriverProfile(account1));
        driverProfileRepository.save(buildDriverProfile(account2));
        driverProfileRepository.save(buildDriverProfile(account3));

        driverId1 = account1.getId();
        driverId2 = account2.getId();
        driverId3 = account3.getId();
    }

    @AfterEach
    void cleanUp() {
        driverLocationRepository.deleteAll();
        driverProfileRepository.deleteAll();
        accountRepository.deleteAll();

        redisLocationTemplate.delete(KEY_LOCATION_PREFIX + driverId1);
        redisLocationTemplate.delete(KEY_LOCATION_PREFIX + driverId2);
        redisLocationTemplate.delete(KEY_LOCATION_PREFIX + driverId3);

        redisStatusTemplate.delete(KEY_STATUS_PREFIX + driverId1);
        redisStatusTemplate.delete(KEY_STATUS_PREFIX + driverId2);
        redisStatusTemplate.delete(KEY_STATUS_PREFIX + driverId3);

        redisStatusTemplate.delete(ONLINE_DRIVERS_KEY);
        redisStatusTemplate.delete(GEO_KEY);
    }

    private Account buildAccount() {
        return Account.builder()
                .role(AccountRole.USER)
                .email(UUID.randomUUID() + "@test.com")
                .phone(UUID.randomUUID().toString())
                .passwordHash("hash")
                .build();
    }

    private DriverProfile buildDriverProfile(Account account) {
        return DriverProfile.builder()
                .account(account)
                .firstName("Eugene")
                .lastName("Wick")
                .licenseNumber(UUID.randomUUID().toString())
                .status(DriverStatus.OFFLINE)
                .averageRating(BigDecimal.ZERO)
                .totalTrips(0)
                .verified(true)
                .build();
    }

    private void setupDriverInRedis(Long driverId, DriverStatus status, VehicleClass vehicleClass) {
        DriverLocationDto dto = new DriverLocationDto(
                driverId,
                new LocationDto(CENTER_LNG, CENTER_LAT),
                vehicleClass
        );
        driverCachingService.updateLocation(dto);
        driverCachingService.updateStatus(driverId, status);
    }

    private void expireLocationKey(Long driverId) {
        redisLocationTemplate.delete(KEY_LOCATION_PREFIX + driverId);
    }

    @Nested
    @DisplayName("persistLocationsToDatabase()")
    class PersistLocationsToDatabase {

        @Test
        @DisplayName("Сохраняет только ONLINE водителей в БД")
        void shouldSaveOnlyOnlineDriversToDatabase() {
            setupDriverInRedis(driverId1, DriverStatus.ONLINE, ECONOMY);
            setupDriverInRedis(driverId2, DriverStatus.ONLINE, ECONOMY);
            setupDriverInRedis(driverId3, DriverStatus.OFFLINE, ECONOMY);

            driverLocationService.persistLocationsToDatabase();

            List<DriverLocation> saved = driverLocationRepository.findAll();
            assertThat(saved).hasSize(2);
            assertThat(saved)
                    .extracting(loc -> loc.getDriver().getAccountId())
                    .containsExactlyInAnyOrder(driverId1, driverId2)
                    .doesNotContain(driverId3);
        }

        @Test
        @DisplayName("Ничего не делает и не бросает исключений, если онлайн-водителей нет")
        void shouldDoNothing_whenNoOnlineDrivers() {
            assertThatCode(() -> driverLocationService.persistLocationsToDatabase())
                    .doesNotThrowAnyException();

            assertThat(driverLocationRepository.findAll()).isEmpty();
        }

        @Test
        @DisplayName("Пропускает водителя, если его локация протухла (TTL истёк)")
        void shouldSkipDriver_whenLocationKeyExpired() {
            setupDriverInRedis(driverId1, DriverStatus.ONLINE, ECONOMY);
            expireLocationKey(driverId1);

            driverLocationService.persistLocationsToDatabase();

            assertThat(driverLocationRepository.findAll()).isEmpty();
        }

        @Test
        @DisplayName("Сохраняет vehicleClass вместе с локацией")
        void shouldPersistVehicleClass() {
            setupDriverInRedis(driverId1, DriverStatus.ONLINE, COMFORT);

            driverLocationService.persistLocationsToDatabase();

            List<DriverLocation> saved = driverLocationRepository.findAll();
            assertThat(saved).hasSize(1);
        }

        @Test
        @DisplayName("Сохраняет корректные координаты в БД")
        void shouldPersistCorrectCoordinates() {
            setupDriverInRedis(driverId1, DriverStatus.ONLINE, ECONOMY);

            driverLocationService.persistLocationsToDatabase();

            List<DriverLocation> saved = driverLocationRepository.findAll();
            assertThat(saved).hasSize(1);
            assertThat(saved.getFirst().getLongitude())
                    .isEqualByComparingTo(CENTER_LNG);
            assertThat(saved.getFirst().getLatitude())
                    .isEqualByComparingTo(CENTER_LAT);
        }
    }

    @Nested
    @DisplayName("getNearbyOnlineDrivers()")
    class GetNearbyOnlineDrivers {

        @Test
        @DisplayName("Возвращает результат из Redis, не трогая БД")
        void shouldReturnFromRedis_withoutTouchingDatabase() {
            setupDriverInRedis(driverId1, DriverStatus.ONLINE, COMFORT);
            long countBefore = driverLocationRepository.count();

            List<DriverLocationDto> result = driverLocationService
                    .getNearbyOnlineDrivers(CENTER_LNG, CENTER_LAT, SEARCH_RADIUS, COMFORT);

            assertThat(result).hasSize(1);
            assertThat(driverLocationRepository.count()).isEqualTo(countBefore);
        }

        @Test
        @DisplayName("Фильтрует по VehicleClass")
        void shouldFilterByVehicleClass() {
            setupDriverInRedis(driverId1, DriverStatus.ONLINE, ECONOMY);
            setupDriverInRedis(driverId2, DriverStatus.ONLINE, COMFORT);

            List<DriverLocationDto> result = driverLocationService
                    .getNearbyOnlineDrivers(CENTER_LNG, CENTER_LAT, SEARCH_RADIUS, ECONOMY);

            assertThat(result).hasSize(1);
            assertThat(result.getFirst().vehicleClass()).isEqualTo(ECONOMY);
        }
    }
}
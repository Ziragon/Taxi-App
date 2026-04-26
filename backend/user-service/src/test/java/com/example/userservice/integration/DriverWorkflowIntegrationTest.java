package com.example.userservice.integration;

import com.example.userservice.dto.data.AuthDto;
import com.example.userservice.dto.data.DriverProfileDto;
import com.example.userservice.dto.data.VehicleDto;
import com.example.userservice.entity.enums.DriverStatus;
import com.example.userservice.entity.enums.VehicleClass;
import com.example.userservice.repository.AccountRepository;
import com.example.userservice.repository.DriverProfileRepository;
import com.example.userservice.repository.VehicleRepository;
import com.example.userservice.service.AuthService;
import com.example.userservice.service.DriverProfileService;
import com.example.userservice.service.VehicleService;
import com.example.userservice.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("Driver Workflow Integration Tests")
class DriverWorkflowIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private DriverProfileService driverProfileService;

    @Autowired
    private VehicleService vehicleService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private DriverProfileRepository driverProfileRepository;

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private jakarta.persistence.EntityManager entityManager;

    @BeforeEach
    void cleanup() {
        vehicleRepository.deleteAll();
        driverProfileRepository.deleteAll();
        accountRepository.deleteAll();
    }

    @Test
    @DisplayName("Полный workflow водителя: регистрация -> профиль -> транспорт -> статус -> верификация")
    void fullDriverWorkflow() {
        AuthDto result = authService.register(
                "driver@workflow.com",
                "+79993333333",
                "DriverPass789"
        );

        Long driverId = jwtUtil.extractAccountId(result.accessTokenDto().token());

        DriverProfileDto profile = driverProfileService.createProfile(
                driverId,
                "Александр",
                "Александров",
                "7799887766",
                null
        );

        assertThat(profile.verified()).isFalse();
        assertThat(profile.status()).isEqualTo(DriverStatus.OFFLINE);

        VehicleDto vehicle1 = vehicleService.addVehicle(
                driverId,
                "Toyota",
                "Camry",
                (short) 2020,
                "Black",
                "A111AA777",
                VehicleClass.COMFORT
        );

        VehicleDto vehicle2 = vehicleService.addVehicle(
                driverId,
                "Hyundai",
                "Solaris",
                (short) 2021,
                "White",
                "B222BB777",
                VehicleClass.ECONOMY
        );

        assertThat(vehicle1.active()).isFalse();
        assertThat(vehicle2.active()).isFalse();

        vehicleService.setActiveVehicle(driverId, vehicle1.id());
        entityManager.flush();
        entityManager.clear();

        List<VehicleDto> vehicles = vehicleService.getVehiclesByDriver(driverId);
        assertThat(vehicles).hasSize(2);
        assertThat(vehicles.stream().filter(VehicleDto::active)).hasSize(1);
        assertThat(vehicles.stream().filter(VehicleDto::active).findFirst().get().id())
                .isEqualTo(vehicle1.id());

        driverProfileService.verifyDriver(driverId);
        driverProfileService.updateStatus(driverId, DriverStatus.ONLINE);
        entityManager.flush();
        entityManager.clear();

        DriverProfileDto updatedProfile = driverProfileService.getProfile(driverId);
        assertThat(updatedProfile.status()).isEqualTo(DriverStatus.ONLINE);

        driverProfileService.verifyDriver(driverId);
        DriverProfileDto verifiedProfile = driverProfileService.getProfile(driverId);
        assertThat(verifiedProfile.verified()).isTrue();
    }

    @Test
    @DisplayName("Несколько автомобилей: только один может быть активным")
    void multipleVehicles_OnlyOneActive() {
        AuthDto result = authService.register(
                "driver2@workflow.com",
                "+79994444444",
                "Pass123"
        );

        Long driverId = jwtUtil.extractAccountId(result.accessTokenDto().token());

        driverProfileService.createProfile(driverId, "Иван", "Иванов", "1122334455", null);

        vehicleService.addVehicle(driverId, "BMW", "X5", (short) 2022, "Gray", "C111CC777", VehicleClass.BUSINESS);
        VehicleDto v2 = vehicleService.addVehicle(driverId, "KIA", "Rio", (short) 2019, "Red", "D222DD777", VehicleClass.ECONOMY);
        VehicleDto v3 = vehicleService.addVehicle(driverId, "Mercedes", "E-Class", (short) 2023, "Silver", "E333EE777", VehicleClass.BUSINESS);

        vehicleService.setActiveVehicle(driverId, v2.id());
        entityManager.flush();
        entityManager.clear();

        List<VehicleDto> afterFirstSet = vehicleService.getVehiclesByDriver(driverId);
        assertThat(afterFirstSet.stream().filter(VehicleDto::active)).hasSize(1);
        assertThat(afterFirstSet.stream().filter(VehicleDto::active).findFirst().get().id()).isEqualTo(v2.id());

        vehicleService.setActiveVehicle(driverId, v3.id());
        entityManager.flush();
        entityManager.clear();

        List<VehicleDto> afterSecondSet = vehicleService.getVehiclesByDriver(driverId);
        assertThat(afterSecondSet.stream().filter(VehicleDto::active)).hasSize(1);
        assertThat(afterSecondSet.stream().filter(VehicleDto::active).findFirst().get().id()).isEqualTo(v3.id());
    }
}
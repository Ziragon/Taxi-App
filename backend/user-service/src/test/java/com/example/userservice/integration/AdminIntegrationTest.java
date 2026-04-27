package com.example.userservice.integration;

import com.example.userservice.dto.data.AccountAdminDto;
import com.example.userservice.dto.data.AuthDto;
import com.example.userservice.dto.data.DriverProfileDto;
import com.example.userservice.dto.data.PassengerProfileDto;
import com.example.userservice.entity.Account;
import com.example.userservice.repository.AccountRepository;
import com.example.userservice.repository.DriverProfileRepository;
import com.example.userservice.repository.PassengerProfileRepository;
import com.example.userservice.repository.VehicleRepository;
import com.example.userservice.service.AdminService;
import com.example.userservice.service.AuthService;
import com.example.userservice.service.DriverProfileService;
import com.example.userservice.service.PassengerProfileService;
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
@DisplayName("Admin Operations Integration Tests")
class AdminIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private AdminService adminService;

    @Autowired
    private AuthService authService;

    @Autowired
    private DriverProfileService driverProfileService;

    @Autowired
    private PassengerProfileService passengerProfileService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private DriverProfileRepository driverProfileRepository;

    @Autowired
    private PassengerProfileRepository passengerProfileRepository;

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @BeforeEach
    void cleanup() {
        vehicleRepository.deleteAll();
        driverProfileRepository.deleteAll();
        passengerProfileRepository.deleteAll();
        accountRepository.deleteAll();
    }

    @Test
    @DisplayName("Получение всех аккаунтов: список содержит все созданные аккаунты")
    void getAllAccounts_ReturnsAllAccounts() {
        authService.register("user1@admin.com", "+79991111111", "Pass123");
        authService.register("user2@admin.com", "+79992222222", "Pass123");
        authService.register("user3@admin.com", "+79993333333", "Pass123");

        List<AccountAdminDto> accounts = adminService.getAllAccounts();

        assertThat(accounts)
                .hasSize(3)
                .extracting(AccountAdminDto::email)
                .containsExactlyInAnyOrder("user1@admin.com", "user2@admin.com", "user3@admin.com");
    }

    @Test
    @DisplayName("Блокировка/разблокировка аккаунта: статус корректно меняется")
    void deactivateAndActivateAccount_StatusChanges() {
        AuthDto result = authService.register("lockable@admin.com", "+79994444444", "Pass123");
        Long accountId = jwtUtil.extractAccountId(result.accessTokenDto().token());

        Account beforeDeactivation = accountRepository.findById(accountId).orElseThrow();
        assertThat(beforeDeactivation.isActive()).isTrue();

        adminService.deactivateAccount(accountId);
        Account afterDeactivation = accountRepository.findById(accountId).orElseThrow();
        assertThat(afterDeactivation.isActive()).isFalse();

        adminService.activateAccount(accountId);
        Account afterActivation = accountRepository.findById(accountId).orElseThrow();
        assertThat(afterActivation.isActive()).isTrue();
    }

    @Test
    @DisplayName("Верификация водителя: флаг verified устанавливается")
    void verifyDriver_MarksAsVerified() {
        AuthDto result = authService.register("driver@admin.com", "+79995555555", "DriverPass123");
        Long driverId = jwtUtil.extractAccountId(result.accessTokenDto().token());

        driverProfileService.createProfile(driverId, "Иван", "Иванов", "1234567890", null);

        DriverProfileDto beforeVerify = driverProfileService.getProfile(driverId);
        assertThat(beforeVerify.verified()).isFalse();

        adminService.verifyDriver(driverId);

        DriverProfileDto afterVerify = driverProfileService.getProfile(driverId);
        assertThat(afterVerify.verified()).isTrue();
    }

    @Test
    @DisplayName("Получение всех водителей: список содержит всех водителей с профилями")
    void getAllDrivers_ReturnsAllDriverProfiles() {
        AuthDto driver1 = authService.register("driver1@admin.com", "+79996666666", "Pass123");
        Long driverId1 = jwtUtil.extractAccountId(driver1.accessTokenDto().token());
        driverProfileService.createProfile(driverId1, "Сергей", "Сергеев", "1111111111", null);

        AuthDto driver2 = authService.register("driver2@admin.com", "+79997777777", "Pass123");
        Long driverId2 = jwtUtil.extractAccountId(driver2.accessTokenDto().token());
        driverProfileService.createProfile(driverId2, "Петр", "Петров", "2222222222", null);

        List<DriverProfileDto> drivers = adminService.getAllDrivers();

        assertThat(drivers)
                .hasSize(2)
                .extracting(DriverProfileDto::firstName)
                .containsExactlyInAnyOrder("Сергей", "Петр");
    }

    @Test
    @DisplayName("Получение всех пассажиров: список содержит всех пассажиров с профилями")
    void getAllPassengers_ReturnsAllPassengerProfiles() {
        AuthDto passenger1 = authService.register("passenger1@admin.com", "+79998888888", "Pass123");
        Long passengerId1 = jwtUtil.extractAccountId(passenger1.accessTokenDto().token());
        passengerProfileService.createProfile(passengerId1, "Мария", "Маркова", null);

        AuthDto passenger2 = authService.register("passenger2@admin.com", "+79999999999", "Pass123");
        Long passengerId2 = jwtUtil.extractAccountId(passenger2.accessTokenDto().token());
        passengerProfileService.createProfile(passengerId2, "Анна", "Анискова", null);

        List<PassengerProfileDto> passengers = adminService.getAllPassengers();

        assertThat(passengers)
                .hasSize(2)
                .extracting(PassengerProfileDto::firstName)
                .containsExactlyInAnyOrder("Мария", "Анна");
    }


    @Test
    @DisplayName("Полный сценарий администратора: проверка всех операций")
    void fullAdminScenario() {

        AuthDto driver1 = authService.register("admin.driver1@test.com", "+79991010101", "Pass123");
        Long driverId1 = jwtUtil.extractAccountId(driver1.accessTokenDto().token());
        driverProfileService.createProfile(driverId1, "Иван", "Иванов", "1111111111", null);

        AuthDto driver2 = authService.register("admin.driver2@test.com", "+79991010102", "Pass123");
        Long driverId2 = jwtUtil.extractAccountId(driver2.accessTokenDto().token());
        driverProfileService.createProfile(driverId2, "Петр", "Петров", "2222222222", null);

        AuthDto passenger = authService.register("admin.passenger@test.com", "+79991010103", "Pass123");
        Long passengerId = jwtUtil.extractAccountId(passenger.accessTokenDto().token());
        passengerProfileService.createProfile(passengerId, "Мария", "Маркова", null);

        List<AccountAdminDto> allAccounts = adminService.getAllAccounts();
        assertThat(allAccounts).hasSize(3);

        List<DriverProfileDto> allDrivers = adminService.getAllDrivers();
        assertThat(allDrivers).hasSize(2);

        List<PassengerProfileDto> allPassengers = adminService.getAllPassengers();
        assertThat(allPassengers).hasSize(1);

        assertThat(allDrivers.getFirst().verified()).isFalse();
        adminService.verifyDriver(driverId1);
        DriverProfileDto verifiedDriver = driverProfileService.getProfile(driverId1);
        assertThat(verifiedDriver.verified()).isTrue();

        Account accountBeforeBlock = accountRepository.findById(passengerId).orElseThrow();
        assertThat(accountBeforeBlock.isActive()).isTrue();
        adminService.deactivateAccount(passengerId);
        Account accountAfterBlock = accountRepository.findById(passengerId).orElseThrow();
        assertThat(accountAfterBlock.isActive()).isFalse();
    }
}
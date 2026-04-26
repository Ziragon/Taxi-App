package com.example.userservice.integration;

import com.example.userservice.dto.data.AuthDto;
import com.example.userservice.dto.data.PassengerProfileDto;
import com.example.userservice.repository.AccountRepository;
import com.example.userservice.repository.PassengerProfileRepository;
import com.example.userservice.service.AuthService;
import com.example.userservice.service.PassengerProfileService;
import com.example.userservice.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("Passenger Workflow Integration Tests")
class PassengerWorkflowIntegrationTest extends BaseIntegrationTest{

    @Autowired
    private AuthService authService;

    @Autowired
    private PassengerProfileService passengerProfileService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private PassengerProfileRepository passengerProfileRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @BeforeEach
    void cleanup() {
        passengerProfileRepository.deleteAll();
        accountRepository.deleteAll();
    }

    @Test
    @DisplayName("Полный workflow пассажира: регистрация -> профиль -> обновление -> рейтинг")
    void fullPassengerWorkflow() {
        AuthDto result = authService.register(
                "passenger@workflow.com",
                "+79995555555",
                "PassPass123"
        );

        Long passengerId = jwtUtil.extractAccountId(result.accessTokenDto().token());

        PassengerProfileDto profile = passengerProfileService.createProfile(
                passengerId,
                "Мария",
                "Иванова",
                "https://cdn.example.com/maria.jpg"
        );

        assertThat(profile.firstName()).isEqualTo("Мария");
        assertThat(profile.averageRating()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(profile.totalTrips()).isZero();

        PassengerProfileDto updated = passengerProfileService.updateProfile(
                passengerId,
                "Мария",
                "Петрова",
                "https://cdn.example.com/maria-new.jpg"
        );

        assertThat(updated.lastName()).isEqualTo("Петрова");

        passengerProfileService.updateRating(passengerId, new BigDecimal("5.00"));
        passengerProfileService.updateRating(passengerId, new BigDecimal("4.00"));
        passengerProfileService.updateRating(passengerId, new BigDecimal("5.00"));

        PassengerProfileDto afterRatings = passengerProfileService.getProfile(passengerId);
        assertThat(afterRatings.totalTrips()).isEqualTo(3);
        assertThat(afterRatings.averageRating()).isEqualByComparingTo("4.67");
    }

    @Test
    @DisplayName("Пересчёт рейтинга: математическая точность")
    void ratingCalculation_Precision() {
        AuthDto result = authService.register(
                "rating@test.com",
                "+79996666666",
                "Test123"
        );

        Long passengerId = jwtUtil.extractAccountId(result.accessTokenDto().token());
        passengerProfileService.createProfile(passengerId, "Test", "User", null);

        passengerProfileService.updateRating(passengerId, new BigDecimal("4.50"));
        PassengerProfileDto after1 = passengerProfileService.getProfile(passengerId);
        assertThat(after1.averageRating()).isEqualByComparingTo("4.50");
        assertThat(after1.totalTrips()).isEqualTo(1);

        passengerProfileService.updateRating(passengerId, new BigDecimal("5.00"));
        PassengerProfileDto after2 = passengerProfileService.getProfile(passengerId);
        assertThat(after2.averageRating()).isEqualByComparingTo("4.75");
        assertThat(after2.totalTrips()).isEqualTo(2);

        passengerProfileService.updateRating(passengerId, new BigDecimal("3.00"));
        PassengerProfileDto after3 = passengerProfileService.getProfile(passengerId);
        assertThat(after3.averageRating()).isEqualByComparingTo("4.17");
        assertThat(after3.totalTrips()).isEqualTo(3);
    }
}
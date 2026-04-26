package com.example.userservice.integration;

import com.example.userservice.dto.data.AuthResult;
import com.example.userservice.entity.PassengerProfile;
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
    private jakarta.persistence.EntityManager entityManager;

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
        AuthResult result = authService.register(
                "passenger@workflow.com",
                "+79995555555",
                "PassPass123"
        );

        Long passengerId = jwtUtil.extractAccountId(result.accessTokenData().token());

        PassengerProfile profile = passengerProfileService.createProfile(
                passengerId,
                "Мария",
                "Иванова",
                "https://cdn.example.com/maria.jpg"
        );

        assertThat(profile.getFirstName()).isEqualTo("Мария");
        assertThat(profile.getAverageRating()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(profile.getTotalTrips()).isZero();

        PassengerProfile updated = passengerProfileService.updateProfile(
                passengerId,
                "Мария",
                "Петрова",
                "https://cdn.example.com/maria-new.jpg"
        );

        assertThat(updated.getLastName()).isEqualTo("Петрова");

        passengerProfileService.updateRating(passengerId, new BigDecimal("5.00"));
        passengerProfileService.updateRating(passengerId, new BigDecimal("4.00"));
        passengerProfileService.updateRating(passengerId, new BigDecimal("5.00"));

        entityManager.flush();
        entityManager.clear();

        PassengerProfile afterRatings = passengerProfileService.getProfile(passengerId);
        assertThat(afterRatings.getTotalTrips()).isEqualTo(3);
        assertThat(afterRatings.getAverageRating()).isEqualByComparingTo("4.67");
    }

    @Test
    @DisplayName("Пересчёт рейтинга: математическая точность")
    void ratingCalculation_Precision() {
        AuthResult result = authService.register(
                "rating@test.com",
                "+79996666666",
                "Test123"
        );

        Long passengerId = jwtUtil.extractAccountId(result.accessTokenData().token());
        passengerProfileService.createProfile(passengerId, "Test", "User", null);

        passengerProfileService.updateRating(passengerId, new BigDecimal("4.50"));
        entityManager.flush();
        entityManager.clear();
        PassengerProfile after1 = passengerProfileService.getProfile(passengerId);
        assertThat(after1.getAverageRating()).isEqualByComparingTo("4.50");
        assertThat(after1.getTotalTrips()).isEqualTo(1);

        passengerProfileService.updateRating(passengerId, new BigDecimal("5.00"));
        entityManager.flush();
        entityManager.clear();
        PassengerProfile after2 = passengerProfileService.getProfile(passengerId);
        assertThat(after2.getAverageRating()).isEqualByComparingTo("4.75");
        assertThat(after2.getTotalTrips()).isEqualTo(2);

        passengerProfileService.updateRating(passengerId, new BigDecimal("3.00"));
        entityManager.flush();
        entityManager.clear();
        PassengerProfile after3 = passengerProfileService.getProfile(passengerId);
        assertThat(after3.getAverageRating()).isEqualByComparingTo("4.17");
        assertThat(after3.getTotalTrips()).isEqualTo(3);
    }
}
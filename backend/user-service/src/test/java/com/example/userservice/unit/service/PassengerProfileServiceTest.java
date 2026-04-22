package com.example.userservice.unit.service;

import com.example.userservice.entity.Account;
import com.example.userservice.entity.PassengerProfile;
import com.example.userservice.entity.enums.AccountRole;
import com.example.userservice.exception.ProfileNotFoundException;
import com.example.userservice.repository.PassengerProfileRepository;
import com.example.userservice.service.AccountService;
import com.example.userservice.service.PassengerProfileService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PassengerProfileService Unit Tests")
class PassengerProfileServiceTest {

    @Mock
    private PassengerProfileRepository passengerProfileRepository;

    @Mock
    private AccountService accountService;

    @InjectMocks
    private PassengerProfileService passengerProfileService;

    @Test
    @DisplayName("Создание профиля: успешное сохранение")
    void createProfile_Success() {
        Account account = Account.builder().id(1L).role(AccountRole.USER).build();
        PassengerProfile profile = PassengerProfile.builder()
                .accountId(1L)
                .firstName("Иван")
                .lastName("Иванов")
                .build();

        when(accountService.findById(1L)).thenReturn(account);
        when(passengerProfileRepository.save(any(PassengerProfile.class))).thenReturn(profile);

        PassengerProfile created = passengerProfileService.createProfile(1L, "Иван", "Иванов", null);

        assertThat(created.getFirstName()).isEqualTo("Иван");
        assertThat(created.getLastName()).isEqualTo("Иванов");
        verify(passengerProfileRepository).save(any(PassengerProfile.class));
    }

    @Test
    @DisplayName("Обновление рейтинга: корректный пересчёт среднего")
    void updateRating_CalculatesCorrectAverage() {
        PassengerProfile profile = PassengerProfile.builder()
                .accountId(1L)
                .averageRating(new BigDecimal("4.50"))
                .totalTrips(2)
                .build();

        when(passengerProfileRepository.findById(1L)).thenReturn(Optional.of(profile));
        when(passengerProfileRepository.save(any(PassengerProfile.class))).thenAnswer(i -> i.getArgument(0));

        passengerProfileService.updateRating(1L, new BigDecimal("5.00"));

        assertThat(profile.getAverageRating()).isEqualByComparingTo("4.67");
        assertThat(profile.getTotalTrips()).isEqualTo(3);
    }

    @Test
    @DisplayName("Получение профиля: профиль не найден -> ProfileNotFoundException")
    void getProfile_NotFound() {
        when(passengerProfileRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> passengerProfileService.getProfile(999L))
                .isInstanceOf(ProfileNotFoundException.class)
                .hasMessageContaining("Passenger profile");
    }
}

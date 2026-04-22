package com.example.userservice.unit.service;

import com.example.userservice.entity.Account;
import com.example.userservice.entity.DriverProfile;
import com.example.userservice.entity.enums.AccountRole;
import com.example.userservice.entity.enums.DriverStatus;
import com.example.userservice.repository.DriverProfileRepository;
import com.example.userservice.service.AccountService;
import com.example.userservice.service.DriverProfileService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DriverProfileService Unit Tests")
class DriverProfileServiceTest {

    @Mock
    private DriverProfileRepository driverProfileRepository;

    @Mock
    private AccountService accountService;

    @InjectMocks
    private DriverProfileService driverProfileService;

    @Test
    @DisplayName("Создание профиля водителя: is_verified = false по умолчанию")
    void createProfile_DefaultsNotVerified() {
        Account account = Account.builder().id(1L).role(AccountRole.USER).build();
        DriverProfile profile = DriverProfile.builder()
                .accountId(1L)
                .firstName("Сергей")
                .licenseNumber("7712345678")
                .verified(false)
                .status(DriverStatus.OFFLINE)
                .build();

        when(accountService.findById(1L)).thenReturn(account);
        when(driverProfileRepository.save(any(DriverProfile.class))).thenReturn(profile);

        DriverProfile created = driverProfileService.createProfile(1L, "Сергей", "Сидоров", "7712345678", null);

        assertThat(created.isVerified()).isFalse();
        assertThat(created.getStatus()).isEqualTo(DriverStatus.OFFLINE);
    }

    @Test
    @DisplayName("Верификация водителя: is_verified становится true")
    void verifyDriver_SetsVerifiedTrue() {
        DriverProfile profile = DriverProfile.builder()
                .accountId(1L)
                .verified(false)
                .build();

        when(driverProfileRepository.findById(1L)).thenReturn(Optional.of(profile));
        when(driverProfileRepository.save(any(DriverProfile.class))).thenAnswer(i -> i.getArgument(0));

        driverProfileService.verifyDriver(1L);

        assertThat(profile.isVerified()).isTrue();
        verify(driverProfileRepository).save(profile);
    }

    @Test
    @DisplayName("Обновление статуса: использует batch update")
    void updateStatus_UsesRepositoryUpdateMethod() {
        when(driverProfileRepository.updateStatus(1L, DriverStatus.ONLINE)).thenReturn(1);

        driverProfileService.updateStatus(1L, DriverStatus.ONLINE);

        verify(driverProfileRepository).updateStatus(1L, DriverStatus.ONLINE);
    }
}

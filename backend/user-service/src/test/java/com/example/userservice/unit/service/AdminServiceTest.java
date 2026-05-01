package com.example.userservice.unit.service;

import com.example.userservice.dto.data.AccountAdminDto;
import com.example.userservice.entity.Account;
import com.example.userservice.entity.DriverProfile;
import com.example.userservice.entity.enums.AccountRole;
import com.example.shared.dto.enums.DriverStatus;
import com.example.userservice.repository.AccountRepository;
import com.example.userservice.repository.DriverProfileRepository;
import com.example.userservice.service.AccountService;
import com.example.userservice.service.AdminService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminService Unit Tests")
class AdminServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private DriverProfileRepository driverProfileRepository;

    @Mock
    private AccountService accountService;

    @InjectMocks
    private AdminService adminService;

    @Test
    @DisplayName("Получение всех аккаунтов: маппинг корректный")
    void getAllAccounts_MapsCorrectly() {
        Account account1 = Account.builder()
                .id(1L)
                .email("admin1@test.com")
                .phone("+79991111111")
                .role(AccountRole.USER)
                .active(true)
                .build();

        Account account2 = Account.builder()
                .id(2L)
                .email("admin2@test.com")
                .phone("+79992222222")
                .role(AccountRole.USER)
                .active(false)
                .build();

        when(accountRepository.findAll()).thenReturn(List.of(account1, account2));

        List<AccountAdminDto> result = adminService.getAllAccounts();

        assertThat(result)
                .hasSize(2)
                .extracting(AccountAdminDto::email)
                .containsExactlyInAnyOrder("admin1@test.com", "admin2@test.com");

        verify(accountRepository).findAll();
    }

    @Test
    @DisplayName("Верификация водителя: сохранение корректное")
    void verifyDriver_SavesCorrectly() {
        Account account = Account.builder().id(1L).build();
        DriverProfile driver = DriverProfile.builder()
                .accountId(1L)
                .account(account)
                .firstName("Ivan")
                .verified(false)
                .status(DriverStatus.OFFLINE)
                .build();

        when(driverProfileRepository.findById(1L)).thenReturn(Optional.of(driver));

        adminService.verifyDriver(1L);

        assertThat(driver.isVerified()).isTrue();
        verify(driverProfileRepository).save(driver);
    }

    @Test
    @DisplayName("Деактивация аккаунта: вызывает accountService")
    void deactivateAccount_CallsAccountService() {
        adminService.deactivateAccount(1L);

        verify(accountService).deactivateAccount(1L);
    }

    @Test
    @DisplayName("Активация аккаунта: вызывает accountService")
    void activateAccount_CallsAccountService() {
        adminService.activateAccount(1L);

        verify(accountService).activateAccount(1L);
    }
}

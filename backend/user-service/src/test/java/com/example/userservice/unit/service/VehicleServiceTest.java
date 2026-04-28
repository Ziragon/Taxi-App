package com.example.userservice.unit.service;

import com.example.userservice.dto.data.VehicleDto;
import com.example.userservice.entity.Account;
import com.example.userservice.entity.DriverProfile;
import com.example.userservice.entity.Vehicle;
import com.example.shared.dto.enums.VehicleClass;
import com.example.userservice.repository.DriverProfileRepository;
import com.example.userservice.repository.VehicleRepository;
import com.example.userservice.service.VehicleService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("VehicleService Unit Tests")
class VehicleServiceTest {

    @Mock
    private VehicleRepository vehicleRepository;

    @Mock
    private DriverProfileRepository driverProfileRepository;

    @InjectMocks
    private VehicleService vehicleService;

    @Test
    @DisplayName("Добавление автомобиля: is_active = false по умолчанию")
    void addVehicle_DefaultsInactive() {
        DriverProfile driver = DriverProfile.builder().accountId(1L).build();
        Vehicle vehicle = Vehicle.builder()
                .id(1L)
                .driver(driver)
                .brand("Toyota")
                .active(false)
                .build();

        when(driverProfileRepository.findById(1L)).thenReturn(Optional.of(driver));
        when(vehicleRepository.existsByLicensePlate("A123BC777")).thenReturn(false);
        when(vehicleRepository.save(any(Vehicle.class))).thenReturn(vehicle);

        VehicleDto created = vehicleService.addVehicle(1L, "Toyota", "Camry", (short) 2020, "Black", "A123BC777", VehicleClass.COMFORT);

        assertThat(created.active()).isFalse();
        verify(vehicleRepository).existsByLicensePlate("A123BC777");
        verify(driverProfileRepository).findById(1L);
        verify(vehicleRepository).save(any(Vehicle.class));
    }

    @Test
    @DisplayName("Установка активного автомобиля: только один активен")
    void setActiveVehicle_OnlyOneActive() {
        DriverProfile driver = DriverProfile.builder().accountId(1L).build();
        Account account = Account.builder().id(1L).build();
        driver.setAccount(account);

        Vehicle vehicle1 = Vehicle.builder().id(1L).active(false).driver(driver).build();
        Vehicle vehicle2 = Vehicle.builder().id(2L).active(true).driver(driver).build();
        Vehicle vehicle3 = Vehicle.builder().id(3L).active(false).driver(driver).build();

        when(vehicleRepository.findAllByDriverAccountId(1L)).thenReturn(List.of(vehicle1, vehicle2, vehicle3));

        vehicleService.setActiveVehicle(1L, 3L);

        assertThat(vehicle1.isActive()).isFalse();
        assertThat(vehicle2.isActive()).isFalse();
        assertThat(vehicle3.isActive()).isTrue();
        verify(vehicleRepository).findAllByDriverAccountId(1L);
        verify(vehicleRepository, times(3)).save(any(Vehicle.class));
        verify(vehicleRepository).flush();
    }
}
package com.example.userservice.service;

import com.example.shared.exception.common.ResourceNotFoundException;
import com.example.userservice.dto.data.*;
import com.example.userservice.dto.data.AccountAdminDto;
import com.example.userservice.entity.DriverProfile;
import com.example.userservice.entity.Vehicle;
import com.example.userservice.repository.AccountRepository;
import com.example.userservice.repository.DriverProfileRepository;
import com.example.userservice.repository.PassengerProfileRepository;
import com.example.userservice.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final AccountRepository accountRepository;
    private final DriverProfileRepository driverProfileRepository;
    private final PassengerProfileRepository passengerProfileRepository;
    private final VehicleRepository vehicleRepository;
    private final AccountService accountService;

    @Transactional(readOnly = true)
    public List<AccountAdminDto> getAllAccounts() {
        return accountRepository.findAll().stream()
                .map(AccountAdminDto::from)
                .toList();
    }

    @Transactional
    public void deactivateAccount(Long accountId) {
        accountService.deactivateAccount(accountId);
    }

    @Transactional
    public void activateAccount(Long accountId) {
        accountService.activateAccount(accountId);
    }

    @Transactional(readOnly = true)
    public List<DriverProfileDto> getAllDrivers() {
        return driverProfileRepository.findAllWithAccount().stream()
                .map(DriverProfileDto::from)
                .toList();
    }

    @Transactional
    public void verifyDriver(Long driverId) {
        DriverProfile profile = driverProfileRepository.findById(driverId)
                .orElseThrow(() -> new ResourceNotFoundException("Driver profile", driverId));

        profile.setVerified(true);
        driverProfileRepository.save(profile);
    }

    @Transactional(readOnly = true)
    public List<PassengerProfileDto> getAllPassengers() {
        return passengerProfileRepository.findAllWithAccount().stream()
                .map(PassengerProfileDto::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<VehicleDto> getAllVehicles() {
        return vehicleRepository.findAllWithDriver().stream()
                .map(VehicleDto::from)
                .toList();
    }

    @Transactional
    public void verifyVehicle(Long vehicleId) {
        Vehicle vehicle = vehicleRepository.findByIdWithDriver(vehicleId)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle", vehicleId));

        vehicle.setVerified(true);
        vehicleRepository.save(vehicle);
    }

    @Transactional
    public void deleteVehicle(Long vehicleId) {
        if (!vehicleRepository.existsById(vehicleId)) {
            throw new ResourceNotFoundException("Vehicle", vehicleId);
        }
        vehicleRepository.deleteById(vehicleId);
    }
}

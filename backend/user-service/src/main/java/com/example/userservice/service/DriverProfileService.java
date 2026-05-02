package com.example.userservice.service;

import com.example.shared.exception.common.ResourceNotFoundException;
import com.example.userservice.dto.data.DriverProfileDto;
import com.example.userservice.entity.Account;
import com.example.userservice.entity.DriverProfile;
import com.example.shared.dto.enums.DriverStatus;
import com.example.userservice.exception.ProfileAlreadyExistsException;
import com.example.userservice.repository.DriverProfileRepository;
import com.example.userservice.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class DriverProfileService {

    private final DriverProfileRepository driverProfileRepository;
    private final DriverCachingService driverCachingService;
    private final VehicleRepository vehicleRepository;
    private final AccountService accountService;
    private static final String DRIVER_PROFILE = "Driver profile";

    @Transactional
    public DriverProfileDto createProfile(Long accountId, String firstName, String lastName,
                                          String licenseNumber, String photoUrl) {
        if (driverProfileRepository.existsById(accountId)) {
            throw new ProfileAlreadyExistsException("Driver");
        }

        if (driverProfileRepository.existsByLicenseNumber(licenseNumber)) {
            throw new ProfileAlreadyExistsException("Driver profile with this license number");
        }

        Account account = accountService.findById(accountId);

        DriverProfile profile = DriverProfile.builder()
                .account(account)
                .firstName(firstName)
                .lastName(lastName)
                .licenseNumber(licenseNumber)
                .photoUrl(photoUrl)
                .status(DriverStatus.OFFLINE)
                .averageRating(BigDecimal.ZERO)
                .totalTrips(0)
                .verified(false)
                .build();

        DriverProfile saved = driverProfileRepository.save(profile);
        return DriverProfileDto.from(saved);
    }

    @Transactional(readOnly = true)
    public DriverProfileDto getProfile(Long accountId) {
        DriverProfile profile = driverProfileRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException(DRIVER_PROFILE, accountId));

        return DriverProfileDto.from(profile);
    }

    @Transactional
    public DriverProfileDto updateProfile(Long accountId, String firstName, String lastName,
                                          String licenseNumber, String photoUrl) {
        DriverProfile profile = driverProfileRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException(DRIVER_PROFILE, accountId));

        profile.setFirstName(firstName);
        profile.setLastName(lastName);
        profile.setLicenseNumber(licenseNumber);
        profile.setPhotoUrl(photoUrl);

        DriverProfile saved = driverProfileRepository.save(profile);
        return DriverProfileDto.from(saved);
    }

    @Transactional
    public void updateStatus(Long accountId, DriverStatus status) {
        DriverProfile profile = driverProfileRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException(DRIVER_PROFILE, accountId));

        if (status == DriverStatus.ONLINE) {
            validateOnlineRequirements(accountId, profile);
        }

        driverCachingService.updateStatus(accountId, status);
        driverProfileRepository.updateStatus(accountId, status);
    }

    @Transactional
    public void verifyDriver(Long accountId) {
        DriverProfile profile = driverProfileRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException(DRIVER_PROFILE, accountId));

        profile.setVerified(true);
        driverProfileRepository.save(profile);
    }

    @Transactional
    public void updateRating(Long accountId, BigDecimal newTripRating) {
        DriverProfile profile = driverProfileRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException(DRIVER_PROFILE, accountId));

        profile.setAverageRating(com.example.userservice.util.RatingCalculator.calculate(
                profile.getAverageRating(),
                profile.getTotalTrips(),
                newTripRating
        ));
        profile.setTotalTrips(profile.getTotalTrips() + 1);
    }

    private void validateOnlineRequirements(Long accountId, DriverProfile profile) {
        if (!profile.isVerified()) {
            throw new IllegalStateException("Driver must be verified to go online");
        }

        if (!profile.getAccount().isActive()) {
            throw new IllegalStateException("Account is not active");
        }

        if (vehicleRepository.findAllByDriverAccountIdAndActiveTrue(accountId).isEmpty()) {
            throw new IllegalStateException("Driver must have an active vehicle to go online");
        }
    }
}
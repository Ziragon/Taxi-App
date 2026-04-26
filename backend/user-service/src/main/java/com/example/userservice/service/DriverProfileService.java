package com.example.userservice.service;

import com.example.userservice.dto.data.DriverProfileDto;
import com.example.userservice.entity.Account;
import com.example.userservice.entity.DriverProfile;
import com.example.userservice.entity.enums.DriverStatus;
import com.example.userservice.exception.ProfileNotFoundException;
import com.example.userservice.repository.DriverProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DriverProfileService {

    private final DriverProfileRepository driverProfileRepository;
    private final AccountService accountService;

    @Transactional
    public DriverProfileDto createProfile(Long accountId, String firstName, String lastName,
                                       String licenseNumber, String photoUrl) {
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
                .orElseThrow(() -> new ProfileNotFoundException("Driver", accountId));

        return DriverProfileDto.from(profile);
    }

    @Transactional
    public DriverProfileDto updateProfile(Long accountId, String firstName, String lastName,
                                       String licenseNumber, String photoUrl) {

        DriverProfile profile = driverProfileRepository.findById(accountId)
                .orElseThrow(() -> new ProfileNotFoundException("Driver", accountId));

        profile.setFirstName(firstName);
        profile.setLastName(lastName);
        profile.setLicenseNumber(licenseNumber);
        profile.setPhotoUrl(photoUrl);

        DriverProfile saved = driverProfileRepository.save(profile);
        return DriverProfileDto.from(saved);
    }

    @Transactional
    public void updateStatus(Long accountId, DriverStatus status) {
        driverProfileRepository.updateStatus(accountId, status);
    }

    @Transactional
    public void verifyDriver(Long accountId) {

        DriverProfile profile = driverProfileRepository.findById(accountId)
                .orElseThrow(() -> new ProfileNotFoundException("Driver", accountId));

        profile.setVerified(true);
        driverProfileRepository.save(profile);
    }

    @Transactional(readOnly = true)
    public List<DriverProfile> getOnlineDrivers() {
        return driverProfileRepository.findAllByStatus(DriverStatus.ONLINE);
    }

    @Transactional(readOnly = true)
    public List<DriverProfile> getVerifiedDrivers() {
        return driverProfileRepository.findAllByVerifiedTrue();
    }

    @Transactional
    public void updateRating(Long accountId, BigDecimal newTripRating) {

        DriverProfile profile = driverProfileRepository.findById(accountId)
                .orElseThrow(() -> new ProfileNotFoundException("Driver", accountId));

        int totalTrips = profile.getTotalTrips() + 1;
        BigDecimal currentAverage = profile.getAverageRating();

        BigDecimal newAverage = currentAverage
                .multiply(BigDecimal.valueOf(profile.getTotalTrips()))
                .add(newTripRating)
                .divide(BigDecimal.valueOf(totalTrips), 2, RoundingMode.HALF_UP);

        profile.setAverageRating(newAverage);
        profile.setTotalTrips(totalTrips);

        driverProfileRepository.save(profile);
    }
}
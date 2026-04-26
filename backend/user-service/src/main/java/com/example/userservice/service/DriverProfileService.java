package com.example.userservice.service;

import com.example.userservice.entity.Account;
import com.example.userservice.entity.DriverProfile;
import com.example.userservice.entity.enums.DriverStatus;
import com.example.userservice.exception.ProfileAlreadyExistsException;
import com.example.userservice.exception.ProfileNotFoundException;
import com.example.userservice.repository.DriverProfileRepository;
import com.example.userservice.util.RatingCalculator;
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
    public DriverProfile createProfile(Long accountId, String firstName, String lastName,
                                       String licenseNumber, String photoUrl) {
        if (driverProfileRepository.existsByLicenseNumber(licenseNumber)) {
            throw new ProfileAlreadyExistsException("Driver profile with license number already exists");
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

        return driverProfileRepository.save(profile);
    }

    @Transactional(readOnly = true)
    public DriverProfile getProfile(Long accountId) {
        return driverProfileRepository.findById(accountId)
                .orElseThrow(() -> new ProfileNotFoundException("Driver", accountId));
    }

    @Transactional
    public DriverProfile updateProfile(Long accountId, String firstName, String lastName,
                                       String licenseNumber, String photoUrl) {
        DriverProfile profile = getProfile(accountId);

        profile.setFirstName(firstName);
        profile.setLastName(lastName);
        profile.setLicenseNumber(licenseNumber);
        profile.setPhotoUrl(photoUrl);

        return driverProfileRepository.save(profile);
    }

    @Transactional
    public void updateStatus(Long accountId, DriverStatus status) {
        driverProfileRepository.updateStatus(accountId, status);
    }

    @Transactional
    public void verifyDriver(Long accountId) {
        DriverProfile profile = getProfile(accountId);
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
        DriverProfile profile = getProfile(accountId);

        profile.setAverageRating(RatingCalculator.calculate(
                profile.getAverageRating(),
                profile.getTotalTrips(),
                newTripRating
        ));
        profile.setTotalTrips(profile.getTotalTrips() + 1);
    }
}
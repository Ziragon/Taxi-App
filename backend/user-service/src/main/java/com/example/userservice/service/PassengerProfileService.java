package com.example.userservice.service;

import com.example.userservice.dto.data.PassengerProfileDto;
import com.example.userservice.entity.Account;
import com.example.userservice.entity.PassengerProfile;
import com.example.userservice.exception.ProfileNotFoundException;
import com.example.userservice.repository.PassengerProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
public class PassengerProfileService {

    private final PassengerProfileRepository passengerProfileRepository;
    private final AccountService accountService;

    @Transactional
    public PassengerProfileDto createProfile(Long accountId, String firstName, String lastName, String photoUrl) {
        Account account = accountService.findById(accountId);

        PassengerProfile profile = PassengerProfile.builder()
                .account(account)
                .firstName(firstName)
                .lastName(lastName)
                .photoUrl(photoUrl)
                .averageRating(BigDecimal.ZERO)
                .totalTrips(0)
                .build();

        PassengerProfile saved = passengerProfileRepository.save(profile);
        return PassengerProfileDto.from(saved);
    }

    @Transactional(readOnly = true)
    public PassengerProfileDto getProfile(Long accountId) {
        PassengerProfile profile = passengerProfileRepository.findById(accountId)
                .orElseThrow(() -> new ProfileNotFoundException("Passenger", accountId));

        return PassengerProfileDto.from(profile);
    }

    @Transactional
    public PassengerProfileDto updateProfile(Long accountId, String firstName, String lastName, String photoUrl) {
        PassengerProfile profile = passengerProfileRepository.findById(accountId)
                .orElseThrow(() -> new ProfileNotFoundException("Passenger", accountId));

        profile.setFirstName(firstName);
        profile.setLastName(lastName);
        profile.setPhotoUrl(photoUrl);

        PassengerProfile saved = passengerProfileRepository.save(profile);
        return PassengerProfileDto.from(saved);
    }

    @Transactional
    public void updateRating(Long accountId, BigDecimal newTripRating) {
        PassengerProfile profile = passengerProfileRepository.findById(accountId)
                .orElseThrow(() -> new ProfileNotFoundException("Passenger", accountId));

        int totalTrips = profile.getTotalTrips() + 1;
        BigDecimal currentAverage = profile.getAverageRating();

        BigDecimal newAverage = currentAverage
                .multiply(BigDecimal.valueOf(profile.getTotalTrips()))
                .add(newTripRating)
                .divide(BigDecimal.valueOf(totalTrips), 2, RoundingMode.HALF_UP);

        profile.setAverageRating(newAverage);
        profile.setTotalTrips(totalTrips);

        passengerProfileRepository.save(profile);
    }
}

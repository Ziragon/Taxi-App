package com.example.userservice.service;

import com.example.userservice.entity.Account;
import com.example.userservice.entity.PassengerProfile;
import com.example.userservice.exception.ProfileNotFoundException;
import com.example.userservice.repository.PassengerProfileRepository;
import com.example.userservice.util.RatingCalculator;
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
    public PassengerProfile createProfile(Long accountId, String firstName, String lastName, String photoUrl) {
        Account account = accountService.findById(accountId);

        PassengerProfile profile = PassengerProfile.builder()
                .account(account)
                .firstName(firstName)
                .lastName(lastName)
                .photoUrl(photoUrl)
                .averageRating(BigDecimal.ZERO)
                .totalTrips(0)
                .build();

        return passengerProfileRepository.save(profile);
    }

    @Transactional(readOnly = true)
    public PassengerProfile getProfile(Long accountId) {
        return passengerProfileRepository.findById(accountId)
                .orElseThrow(() -> new ProfileNotFoundException("Passenger", accountId));
    }

    @Transactional
    public PassengerProfile updateProfile(Long accountId, String firstName, String lastName, String photoUrl) {
        PassengerProfile profile = getProfile(accountId);

        profile.setFirstName(firstName);
        profile.setLastName(lastName);
        profile.setPhotoUrl(photoUrl);

        return passengerProfileRepository.save(profile);
    }

    @Transactional
    public void updateRating(Long accountId, BigDecimal newTripRating) {
        PassengerProfile profile = getProfile(accountId);

        profile.setAverageRating(RatingCalculator.calculate(
                profile.getAverageRating(),
                profile.getTotalTrips(),
                newTripRating
        ));
        profile.setTotalTrips(profile.getTotalTrips() + 1);
    }
}

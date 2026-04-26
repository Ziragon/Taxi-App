package com.example.userservice.service;

import com.example.shared.exception.common.ResourceNotFoundException;
import com.example.userservice.entity.Account;
import com.example.userservice.entity.PassengerProfile;
import com.example.userservice.exception.ProfileAlreadyExistsException;
import com.example.userservice.repository.PassengerProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class PassengerProfileService {

    private final PassengerProfileRepository passengerProfileRepository;
    private final AccountService accountService;

    @Transactional
    public PassengerProfile createProfile(Long accountId, String firstName, String lastName, String photoUrl) {
        if (passengerProfileRepository.existsById(accountId)) {
            throw new ProfileAlreadyExistsException("Passenger");
        }

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
                .orElseThrow(() -> new ResourceNotFoundException("Passenger profile", accountId));
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
        if (!passengerProfileRepository.existsById(accountId)) {
            throw new ResourceNotFoundException("Passenger profile", accountId);
        }
        passengerProfileRepository.updateRating(accountId, newTripRating);
    }
}

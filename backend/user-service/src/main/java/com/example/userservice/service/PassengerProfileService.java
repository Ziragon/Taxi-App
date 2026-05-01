package com.example.userservice.service;

import com.example.shared.exception.common.ResourceNotFoundException;
import com.example.userservice.dto.data.PassengerProfileDto;
import com.example.userservice.entity.Account;
import com.example.userservice.entity.PassengerProfile;
import com.example.userservice.exception.ProfileAlreadyExistsException;
import com.example.userservice.exception.ProfileNotFoundException;
import com.example.userservice.repository.PassengerProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class PassengerProfileService {

    private final PassengerProfileRepository passengerProfileRepository;
    private final AccountService accountService;

    private static final String PASSENGER_PROFILE = "Passenger profile";

    @Transactional
    public PassengerProfileDto createProfile(Long accountId, String firstName, String lastName, String photoUrl) {
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

        PassengerProfile saved = passengerProfileRepository.save(profile);
        return PassengerProfileDto.from(saved);
    }

    @Transactional(readOnly = true)
    public PassengerProfileDto getProfile(Long accountId) {
        PassengerProfile profile = passengerProfileRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException(PASSENGER_PROFILE, accountId));

        return PassengerProfileDto.from(profile);
    }

    @Transactional
    public PassengerProfileDto updateProfile(Long accountId, String firstName, String lastName, String photoUrl) {
        PassengerProfile profile = passengerProfileRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException(PASSENGER_PROFILE, accountId));

        profile.setFirstName(firstName);
        profile.setLastName(lastName);
        profile.setPhotoUrl(photoUrl);

        PassengerProfile saved = passengerProfileRepository.save(profile);
        return PassengerProfileDto.from(saved);
    }

    @Transactional
    public void updateRating(Long accountId, BigDecimal newTripRating) {
        if (!passengerProfileRepository.existsById(accountId)) {
            throw new ResourceNotFoundException(PASSENGER_PROFILE, accountId);
        }
        passengerProfileRepository.updateRating(accountId, newTripRating);
    }
}

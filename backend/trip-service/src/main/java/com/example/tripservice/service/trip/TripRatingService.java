package com.example.tripservice.service.trip;

import com.example.shared.exception.common.AccessDeniedException;
import com.example.tripservice.entity.Rating;
import com.example.tripservice.entity.Trip;
import com.example.tripservice.entity.enums.AccountType;
import com.example.tripservice.entity.enums.TripStatus;
import com.example.tripservice.exception.DuplicateRatingException;
import com.example.tripservice.exception.TripNotCompletedException;
import com.example.tripservice.exception.TripNotFoundException;
import com.example.tripservice.repository.RatingRepository;
import com.example.tripservice.repository.TripRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TripRatingService {

    private final TripRepository tripRepository;
    private final RatingRepository ratingRepository;

    public void rateTrip(Long raterId, Long rateeId, Long tripId, AccountType ratedBy, Integer score, String comment) {

        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new TripNotFoundException(tripId));

        if (trip.getStatus() != TripStatus.COMPLETED) {
            throw new TripNotCompletedException("Assessment may be possible only after completion of the trip");
        }

        validateParticipant(trip, raterId, ratedBy);

        if (ratingRepository.existsByTripIdAndRaterIdAndRateeId(
                tripId, raterId, rateeId)) {
            throw new DuplicateRatingException();
        }

        Rating rating = Rating.builder()
                .trip(trip)
                .ratedBy(ratedBy)
                .raterId(raterId)
                .rateeId(rateeId)
                .score(score)
                .comment(comment)
                .build();

        ratingRepository.save(rating);
    }

    private void validateParticipant(Trip trip, Long raterId, AccountType ratedBy) {
        boolean isValid = switch (ratedBy) {
            case PASSENGER -> trip.getPassengerId().equals(raterId);
            case DRIVER -> trip.getDriverId() != null && trip.getDriverId().equals(raterId);
            case SYSTEM -> throw new AccessDeniedException("System can not give a rating");
        };
        if (!isValid) {
            throw new AccessDeniedException("You are not owner of this trip");
        }
    }
}
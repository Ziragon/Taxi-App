package com.example.tripservice.controller;

import com.example.shared.security.UserPrincipal;
import com.example.tripservice.dto.request.RatingRequest;
import com.example.tripservice.service.trip.TripRatingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/trips/ratings")
@RequiredArgsConstructor
public class RatingController {

    private final TripRatingService tripRatingService;

    @PostMapping
    public ResponseEntity<Void> rate(
            @RequestBody @Valid RatingRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        tripRatingService.rateTrip(principal.userId(), request.rateeId(), request.tripId(), request.accountType(), request.score(), request.comment());
        return ResponseEntity.noContent().build();
    }
}

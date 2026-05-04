package com.example.tripservice.controller;

import com.example.shared.dto.enums.VehicleClass;
import com.example.shared.security.UserPrincipal;
import com.example.tripservice.dto.data.TripCreateDto;
import com.example.tripservice.dto.data.TripDto;
import com.example.tripservice.dto.request.TripCreateRequest;
import com.example.tripservice.dto.response.TripResponse;
import com.example.tripservice.service.trip.TripPassengerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/trips")
@RequiredArgsConstructor
public class TripPassengerController {

    private final TripPassengerService tripPassengerService;

    @PostMapping
    public ResponseEntity<TripResponse> createTrip(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody TripCreateRequest request
    ) {
        TripDto result = tripPassengerService.createTrip(
                principal.userId(), TripCreateDto.from(request));
        return ResponseEntity.ok(TripResponse.from(result));
    }

    @PostMapping("/start-search")
    public ResponseEntity<TripResponse> startSearching(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam VehicleClass vehicleClass
    ) {
        TripDto result = tripPassengerService.startSearching(principal.userId(), vehicleClass);
        return ResponseEntity.ok(TripResponse.from(result));
    }

    @PostMapping("/{tripId}/cancel")
    public ResponseEntity<Void> cancelTrip(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long tripId
    ) {
        tripPassengerService.cancelTrip(tripId, principal.userId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{tripId}")
    public ResponseEntity<TripResponse> getTrip(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long tripId
    ) {
        return ResponseEntity.ok(
                TripResponse.from(tripPassengerService.getTrip(principal.userId(), tripId)));
    }

    @GetMapping("/active")
    public ResponseEntity<TripResponse> getActiveTrip(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long tripId
    ) {
        // TODO
        return ResponseEntity.ok(TripResponse.from(null));
    }
}

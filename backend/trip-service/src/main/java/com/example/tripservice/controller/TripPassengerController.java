package com.example.tripservice.controller;

import com.example.shared.dto.enums.VehicleClass;
import com.example.shared.security.UserPrincipal;
import com.example.tripservice.dto.data.AddressDto;
import com.example.tripservice.dto.data.TripCreateDto;
import com.example.tripservice.dto.data.TripDto;
import com.example.tripservice.dto.request.TripCreateRequest;
import com.example.tripservice.dto.response.TripResponse;
import com.example.tripservice.service.TripService;
import com.example.tripservice.service.TripStatusService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/trips")
@RequiredArgsConstructor
public class TripPassengerController {

    private final TripService tripService;
    private final TripStatusService tripStatusService;

    @PostMapping
    public ResponseEntity<TripResponse> createTrip(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody TripCreateRequest request
    ) {
        TripDto result = tripService.createTrip(principal.userId(), TripCreateDto.from(request));

        return ResponseEntity.ok(TripResponse.from(result));
    }

    @PostMapping("/{tripId}/start-search")
    public ResponseEntity<TripResponse> startSearching(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long tripId,
            @RequestParam VehicleClass vehicleClass
    ) {
        AddressDto dto = tripService.startSearching(principal.userId(), tripId, vehicleClass);
        tripService.beginDriverSearch(tripId, dto.longitude(), dto.latitude(), vehicleClass);

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{tripId}/cancel")
    public ResponseEntity<Void> cancelTrip(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long tripId
    ) {
        tripStatusService.cancelTrip(tripId, principal.userId());

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{tripId}")
    public ResponseEntity<TripResponse> getTrip(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long tripId
    ) {
        TripDto trip = tripService.getTripById(principal.userId(), tripId);
        return ResponseEntity.ok(TripResponse.from(trip));
    }
}

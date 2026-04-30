package com.example.tripservice.controller;

import com.example.shared.dto.enums.VehicleClass;
import com.example.shared.security.UserPrincipal;
import com.example.tripservice.dto.data.TripCreateDto;
import com.example.tripservice.dto.data.TripDto;
import com.example.tripservice.dto.request.TripCreateRequest;
import com.example.tripservice.dto.response.TripResponse;
import com.example.tripservice.service.TripService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/trips")
@RequiredArgsConstructor
public class TripController {

    private final TripService tripService;

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
        tripService.startSearching(principal.userId(), tripId, vehicleClass);

        return ResponseEntity.ok().build();
    }
}

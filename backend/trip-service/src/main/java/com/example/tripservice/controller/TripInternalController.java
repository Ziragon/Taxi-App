package com.example.tripservice.controller;

import com.example.tripservice.service.DriverSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/internal/trips")
@RequiredArgsConstructor
public class TripInternalController {

    private final DriverSearchService driverSearchService;

    @PostMapping("/{tripId}/accept")
    public ResponseEntity<Void> acceptTrip(
            @PathVariable Long tripId,
            @AuthenticationPrincipal Long driverId
    ) {
        driverSearchService.handleDriverAccept(tripId, driverId);

        return ResponseEntity.ok().build();
    }

    @PostMapping("/{tripId}/reject")
    public ResponseEntity<Void> rejectTrip(
            @PathVariable Long tripId,
            @AuthenticationPrincipal Long driverId
    ) {
        driverSearchService.handleDriverReject(tripId, driverId);

        return ResponseEntity.ok().build();
    }
}
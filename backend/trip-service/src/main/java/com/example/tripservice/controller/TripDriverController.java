package com.example.tripservice.controller;

import com.example.shared.security.UserPrincipal;
import com.example.tripservice.service.DriverSearchService;
import com.example.tripservice.service.TripStatusService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/trips")
@RequiredArgsConstructor
public class TripDriverController {

    private final TripStatusService tripStatusService;
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

    @PostMapping("/{tripId}/start")
    public ResponseEntity<Void> startTrip(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long tripId
    ) {
        tripStatusService.startTrip(tripId, principal.userId());

        return ResponseEntity.ok().build();
    }

    @PostMapping("/{tripId}/complete")
    public ResponseEntity<Void> completeTrip(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long tripId
    ) {
        tripStatusService.completeTrip(tripId, principal.userId());

        return ResponseEntity.ok().build();
    }
}

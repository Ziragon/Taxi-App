package com.example.tripservice.controller;

import com.example.shared.security.UserPrincipal;
import com.example.tripservice.dto.data.RouteDto;
import com.example.tripservice.dto.response.DriverCoordinatesRequest;
import com.example.tripservice.dto.response.RouteResponse;
import com.example.tripservice.service.trip.TripDriverService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/trips")
@RequiredArgsConstructor
public class TripDriverController {

    private final TripDriverService tripDriverService;

    @PostMapping("/{tripId}/accept")
    public ResponseEntity<RouteResponse> acceptTrip(
            @PathVariable Long tripId,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody DriverCoordinatesRequest request) {
        RouteDto route = tripDriverService.acceptTrip(
                tripId, principal.userId(), request.longitude(), request.latitude());
        return ResponseEntity.ok(RouteResponse.from(route));
    }

    @PostMapping("/{tripId}/reject")
    public ResponseEntity<Void> rejectTrip(
            @PathVariable Long tripId,
            @AuthenticationPrincipal UserPrincipal principal) {
        tripDriverService.rejectTrip(tripId, principal.userId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{tripId}/start")
    public ResponseEntity<Void> startTrip(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long tripId) {
        tripDriverService.startTrip(tripId, principal.userId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{tripId}/complete")
    public ResponseEntity<Void> completeTrip(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long tripId) {
        tripDriverService.completeTrip(tripId, principal.userId());
        return ResponseEntity.noContent().build();
    }
}

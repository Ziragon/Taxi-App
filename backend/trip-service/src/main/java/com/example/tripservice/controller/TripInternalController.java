package com.example.tripservice.controller;

import com.example.tripservice.service.trip.ActiveTripCacheService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/internal")
@RequiredArgsConstructor
@Tag(name = "Internal Requests (НЕ ДЛЯ ФРОНТА)")
public class TripInternalController {

    private final ActiveTripCacheService cacheService;

    @GetMapping("/driver/{driverId}/status")
    public ResponseEntity<Boolean> hasActiveTrip(
            @PathVariable Long driverId
    ) {
        return ResponseEntity.ok(cacheService.hasActiveTrip(driverId));
    }
}

package com.example.userservice.controller;

import com.example.shared.dto.enums.VehicleClass;
import com.example.shared.dto.data.DriverLocationDto;
import com.example.userservice.dto.request.DriverLocationRequest;
import com.example.userservice.service.DriverLocationService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/internal/drivers")
@RequiredArgsConstructor
@Tag(name = "Internal Requests (НЕ ДЛЯ ФРОНТА)")
public class DriverLocationInternalController {

    private final DriverLocationService driverLocationService;

    @PutMapping("/{driverId}/location")
    public ResponseEntity<Void> updateLocation(
            @PathVariable Long driverId,
            @RequestBody DriverLocationRequest request
    ) {
        driverLocationService.updateLocation(driverId, request.longitude(), request.latitude(), request.vehicleClass());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/nearby")
    public ResponseEntity<List<DriverLocationDto>> getNearbyDrivers(
            @RequestParam BigDecimal lng,
            @RequestParam BigDecimal lat,
            @RequestParam BigDecimal rad,
            @RequestParam(required = false) VehicleClass vehicleClass
    ) {
        return ResponseEntity.ok(
                driverLocationService.getNearbyOnlineDrivers(
                        lng, lat,
                        rad, vehicleClass
                )
        );
    }
}

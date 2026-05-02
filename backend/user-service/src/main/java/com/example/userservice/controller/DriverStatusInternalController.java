package com.example.userservice.controller;

import com.example.shared.dto.enums.DriverStatus;
import com.example.userservice.service.DriverProfileService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/internal/driver")
@RequiredArgsConstructor
@Tag(name = "Internal Requests (НЕ ДЛЯ ФРОНТА)")
public class DriverStatusInternalController {

    private final DriverProfileService driverProfileService;

    @PutMapping("/{driverId}/status/offline")
    public ResponseEntity<Void> setOfflineStatus(
            @PathVariable Long driverId
    ) {
        driverProfileService.updateStatus(driverId, DriverStatus.ONLINE);

        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{driverId}/status/busy")
    public ResponseEntity<Void> setBusyStatus(
            @PathVariable Long driverId
    ) {
        driverProfileService.updateStatus(driverId, DriverStatus.BUSY);

        return ResponseEntity.noContent().build();
    }
}

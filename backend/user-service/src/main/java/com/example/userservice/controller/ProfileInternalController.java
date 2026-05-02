package com.example.userservice.controller;

import com.example.shared.dto.enums.DriverStatus;
import com.example.userservice.repository.PassengerProfileRepository;
import com.example.userservice.service.DriverCachingService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/internal")
@RequiredArgsConstructor
@Tag(name = "Internal Requests (НЕ ДЛЯ ФРОНТА)")
public class ProfileInternalController {

    private final PassengerProfileRepository passengerRepo;
    private final DriverCachingService cachingService;

    @GetMapping("/passenger/{accountId}/exists")
    public ResponseEntity<Boolean> hasPassengerProfile(@PathVariable Long accountId) {
        return ResponseEntity.ok(passengerRepo.existsById(accountId));
    }

    @GetMapping("/driver/{driverId}/status")
    public ResponseEntity<DriverStatus> getDriverStatus(@PathVariable Long driverId) {
        return ResponseEntity.ok(
                cachingService.getStatus(driverId)
        );
    }
}

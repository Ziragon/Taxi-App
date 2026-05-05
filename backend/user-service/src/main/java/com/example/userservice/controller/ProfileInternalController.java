package com.example.userservice.controller;

import com.example.shared.dto.enums.DriverStatus;
import com.example.userservice.dto.response.DriverProfileResponse;
import com.example.userservice.dto.response.PassengerProfileResponse;
import com.example.userservice.repository.PassengerProfileRepository;
import com.example.userservice.service.DriverProfileService;
import com.example.userservice.service.PassengerProfileService;
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
    private final DriverProfileService driverService;
    private final PassengerProfileService passengeService;

    @GetMapping("/passenger/{accountId}/exists")
    public ResponseEntity<Boolean> hasPassengerProfile(@PathVariable Long accountId) {
        return ResponseEntity.ok(passengerRepo.existsById(accountId));
    }

    @GetMapping("/driver/{driverId}/status")
    public ResponseEntity<DriverStatus> getDriverStatus(@PathVariable Long driverId) {
        return ResponseEntity.ok(
                driverService.getStatus(driverId)
        );
    }

    @GetMapping("/driver/{driverId}/profile")
    public ResponseEntity<DriverProfileResponse> getDriverProfile(@PathVariable Long driverId) {
        return ResponseEntity.ok(
                DriverProfileResponse.from(driverService.getProfile(driverId))
        );
    }

    @GetMapping("/passenger/{passengerId}/profile")
    public ResponseEntity<PassengerProfileResponse> getPassengerProfile(@PathVariable Long passengerId) {
        return ResponseEntity.ok(
                PassengerProfileResponse.from(passengeService.getProfile(passengerId))
        );
    }

}

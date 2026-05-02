package com.example.userservice.controller;

import com.example.shared.dto.enums.DriverStatus;
import com.example.shared.security.UserPrincipal;
import com.example.userservice.service.DriverProfileService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/internal")
@RequiredArgsConstructor
@Tag(name = "Internal Requests (НЕ ДЛЯ ФРОНТА)")
public class DriverStatusInternalController {

    private final DriverProfileService driverProfileService;

    @PutMapping()
    public ResponseEntity<Void> setOfflineStatus(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        driverProfileService.updateStatus(principal.userId(), DriverStatus.ONLINE);

        return ResponseEntity.noContent().build();
    }

    @PutMapping()
    public ResponseEntity<Void> setBusyStatus(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        driverProfileService.updateStatus(principal.userId(), DriverStatus.BUSY);

        return ResponseEntity.noContent().build();
    }
}

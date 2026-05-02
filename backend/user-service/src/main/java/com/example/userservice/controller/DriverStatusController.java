package com.example.userservice.controller;

import com.example.shared.dto.enums.DriverStatus;
import com.example.shared.security.UserPrincipal;
import com.example.userservice.dto.response.DriverStatusResponse;
import com.example.userservice.service.DriverProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/driver")
@RequiredArgsConstructor
@Tag(name = "Driver Status", description = "Управление статусами водителей")
public class DriverStatusController {

    private final DriverProfileService driverProfileService;

    @PutMapping("/online")
    @Operation(
            summary = "Начало смены водителя",
            description = "Водитель меняет свой статус на ONLINE",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Статус обновлён")
            }
    )
    public ResponseEntity<Void> setOnlineStatus(
            @AuthenticationPrincipal UserPrincipal principal
    ) {

        driverProfileService.updateStatus(principal.userId(), DriverStatus.ONLINE);

        return ResponseEntity.noContent().build();
    }

    @PutMapping("/offline")
    @Operation(
            summary = "Окончание смены водителя",
            description = "Водитель меняет свой статус на OFFLINE",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Статус обновлён")
            }
    )
    public ResponseEntity<Void> setOfflineStatus(
            @AuthenticationPrincipal UserPrincipal principal
    ) {

        driverProfileService.updateStatus(principal.userId(), DriverStatus.OFFLINE);

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/status")
    public ResponseEntity<DriverStatusResponse> getStatus(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        DriverStatus status = driverProfileService.getStatus(principal.userId());

        return ResponseEntity.ok(new DriverStatusResponse(status));
    }
}

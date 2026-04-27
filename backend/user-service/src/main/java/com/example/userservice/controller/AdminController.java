package com.example.userservice.controller;

import com.example.userservice.dto.data.*;
import com.example.userservice.dto.data.AccountAdminDto;
import com.example.userservice.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ADMIN')")
@Tag(name = "Admin", description = "Административные операции")
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/accounts")
    @Operation(
            summary = "Получить все аккаунты",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Список аккаунтов"),
                    @ApiResponse(responseCode = "403", description = "Недостаточно прав")
            }
    )
    public ResponseEntity<List<AccountAdminDto>> getAllAccounts() {
        return ResponseEntity.ok(adminService.getAllAccounts());
    }

    @PutMapping("/accounts/{accountId}/deactivate")
    @Operation(
            summary = "Заблокировать аккаунт",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Аккаунт заблокирован"),
                    @ApiResponse(responseCode = "404", description = "Аккаунт не найден"),
                    @ApiResponse(responseCode = "403", description = "Недостаточно прав")
            }
    )
    public ResponseEntity<Void> deactivateAccount(@PathVariable Long accountId) {
        adminService.deactivateAccount(accountId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/accounts/{accountId}/activate")
    @Operation(
            summary = "Разблокировать аккаунт",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Аккаунт разблокирован"),
                    @ApiResponse(responseCode = "404", description = "Аккаунт не найден"),
                    @ApiResponse(responseCode = "403", description = "Недостаточно прав")
            }
    )
    public ResponseEntity<Void> activateAccount(@PathVariable Long accountId) {
        adminService.activateAccount(accountId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/drivers")
    @Operation(
            summary = "Получить все профили водителей",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Список водителей"),
                    @ApiResponse(responseCode = "403", description = "Недостаточно прав")
            }
    )
    public ResponseEntity<List<DriverProfileDto>> getAllDrivers() {
        return ResponseEntity.ok(adminService.getAllDrivers());
    }

    @PostMapping("/drivers/{driverId}/verify")
    @Operation(
            summary = "Верифицировать водителя",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Водитель верифицирован"),
                    @ApiResponse(responseCode = "404", description = "Профиль не найден"),
                    @ApiResponse(responseCode = "403", description = "Недостаточно прав")
            }
    )
    public ResponseEntity<Void> verifyDriver(@PathVariable Long driverId) {
        adminService.verifyDriver(driverId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/passengers")
    @Operation(
            summary = "Получить все профили пассажиров",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Список пассажиров"),
                    @ApiResponse(responseCode = "403", description = "Недостаточно прав")
            }
    )
    public ResponseEntity<List<PassengerProfileDto>> getAllPassengers() {
        return ResponseEntity.ok(adminService.getAllPassengers());
    }

    @GetMapping("/vehicles")
    @Operation(
            summary = "Получить все автомобили",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Список автомобилей"),
                    @ApiResponse(responseCode = "403", description = "Недостаточно прав")
            }
    )
    public ResponseEntity<List<VehicleDto>> getAllVehicles() {
        return ResponseEntity.ok(adminService.getAllVehicles());
    }

    @PostMapping("/vehicles/{vehicleId}/verify")
    @Operation(
            summary = "Верифицировать автомобиль",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Автомобиль верифицирован"),
                    @ApiResponse(responseCode = "404", description = "Автомобиль не найден"),
                    @ApiResponse(responseCode = "403", description = "Недостаточно прав")
            }
    )
    public ResponseEntity<Void> verifyVehicle(@PathVariable Long vehicleId) {
        adminService.verifyVehicle(vehicleId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/vehicles/{vehicleId}")
    @Operation(
            summary = "Удалить автомобиль",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Автомобиль удалён"),
                    @ApiResponse(responseCode = "404", description = "Автомобиль не найден"),
                    @ApiResponse(responseCode = "403", description = "Недостаточно прав")
            }
    )
    public ResponseEntity<Void> deleteVehicle(@PathVariable Long vehicleId) {
        adminService.deleteVehicle(vehicleId);
        return ResponseEntity.noContent().build();
    }
}

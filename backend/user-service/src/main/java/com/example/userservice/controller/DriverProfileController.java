package com.example.userservice.controller;

import com.example.userservice.dto.request.CreateDriverProfileRequest;
import com.example.userservice.dto.request.UpdateDriverProfileRequest;
import com.example.userservice.dto.request.UpdateDriverStatusRequest;
import com.example.userservice.dto.response.DriverProfileResponse;
import com.example.userservice.entity.DriverProfile;
import com.example.userservice.service.DriverProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/profiles/driver")
@RequiredArgsConstructor
@Tag(name = "Driver Profile", description = "Управление профилями водителей")
public class DriverProfileController {

    private final DriverProfileService driverProfileService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Создать профиль водителя",
            description = "Создаёт профиль после регистрации. is_verified = false по умолчанию",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "firstName": "Сергей",
                                      "lastName": "Сидоров",
                                      "licenseNumber": "7712345678",
                                      "photoUrl": "https://cdn.example.com/driver.jpg"
                                    }
                                    """)
                    )
            ),
            responses = {
                    @ApiResponse(responseCode = "201", description = "Профиль создан"),
                    @ApiResponse(responseCode = "409", description = "Профиль или номер ВУ уже существует")
            }
    )
    public DriverProfileResponse createProfile(
            @RequestHeader("X-Account-ID") Long accountId,
            @Valid @RequestBody CreateDriverProfileRequest request) {
        DriverProfile profile = driverProfileService.createProfile(
                accountId,
                request.getFirstName(),
                request.getLastName(),
                request.getLicenseNumber(),
                request.getPhotoUrl()
        );
        return mapToResponse(profile);
    }

    @GetMapping
    @Operation(
            summary = "Получить профиль водителя",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Профиль найден"),
                    @ApiResponse(responseCode = "404", description = "Профиль не найден")
            }
    )
    public DriverProfileResponse getProfile(@RequestHeader("X-Account-ID") Long accountId) {
        DriverProfile profile = driverProfileService.getProfile(accountId);
        return mapToResponse(profile);
    }

    @PutMapping
    @Operation(
            summary = "Обновить профиль водителя",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "firstName": "Алексей",
                                      "lastName": "Алексеев",
                                      "licenseNumber": "9988776655",
                                      "photoUrl": "https://cdn.example.com/new-driver.jpg"
                                    }
                                    """)
                    )
            ),
            responses = {
                    @ApiResponse(responseCode = "200", description = "Профиль обновлён")
            }
    )
    public DriverProfileResponse updateProfile(
            @RequestHeader("X-Account-ID") Long accountId,
            @Valid @RequestBody UpdateDriverProfileRequest request) {
        DriverProfile profile = driverProfileService.updateProfile(
                accountId,
                request.getFirstName(),
                request.getLastName(),
                request.getLicenseNumber(),
                request.getPhotoUrl()
        );
        return mapToResponse(profile);
    }

    @PutMapping("/status")
    @Operation(
            summary = "Обновить статус водителя",
            description = "Водитель меняет свой статус (ONLINE/OFFLINE/BUSY)",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "status": "ONLINE"
                                    }
                                    """)
                    )
            ),
            responses = {
                    @ApiResponse(responseCode = "204", description = "Статус обновлён")
            }
    )
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateStatus(
            @RequestHeader("X-Account-ID") Long accountId,
            @Valid @RequestBody UpdateDriverStatusRequest request) {
        driverProfileService.updateStatus(accountId, request.getStatus());
    }

    @PostMapping("/{driverId}/verify")
    @Operation(
            summary = "Верифицировать водителя (только для админа)",
            description = "Устанавливает is_verified = true",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Водитель верифицирован"),
                    @ApiResponse(responseCode = "403", description = "Недостаточно прав")
            }
    )
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void verifyDriver(@PathVariable Long driverId) {
        driverProfileService.verifyDriver(driverId);
    }

    private DriverProfileResponse mapToResponse(DriverProfile profile) {
        return new DriverProfileResponse(
                profile.getAccountId(),
                profile.getFirstName(),
                profile.getLastName(),
                profile.getPhotoUrl(),
                profile.getLicenseNumber(),
                profile.getStatus(),
                profile.getAverageRating(),
                profile.getTotalTrips(),
                profile.getIsVerified()
        );
    }
}

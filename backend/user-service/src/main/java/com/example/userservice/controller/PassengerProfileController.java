package com.example.userservice.controller;

import com.example.userservice.dto.request.CreatePassengerProfileRequest;
import com.example.userservice.dto.request.UpdatePassengerProfileRequest;
import com.example.userservice.dto.response.PassengerProfileResponse;
import com.example.userservice.entity.PassengerProfile;
import com.example.userservice.service.PassengerProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/profiles/passenger")
@RequiredArgsConstructor
@Tag(name = "Passenger Profile", description = "Управление профилями пассажиров")
public class PassengerProfileController {

    private final PassengerProfileService passengerProfileService;

    @PostMapping
    @Operation(
            summary = "Создать профиль пассажира",
            description = "Создаёт профиль после регистрации. Вызывается когда пользователь заполнил анкету",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "firstName": "Иван",
                                      "lastName": "Иванов",
                                      "photoUrl": "https://cdn.example.com/avatar.jpg"
                                    }
                                    """)
                    )
            ),
            responses = {
                    @ApiResponse(responseCode = "200", description = "Профиль создан"),
                    @ApiResponse(responseCode = "404", description = "Аккаунт не найден"),
                    @ApiResponse(responseCode = "409", description = "Профиль уже существует")
            }
    )
    public ResponseEntity<PassengerProfileResponse> createProfile(
            @RequestHeader("X-Account-ID") Long accountId,
            @Valid @RequestBody CreatePassengerProfileRequest request
    ) {
        PassengerProfile profile = passengerProfileService.createProfile(
                accountId,
                request.firstName(),
                request.lastName(),
                request.photoUrl()
        );

        return ResponseEntity.ok(PassengerProfileResponse.from(profile));
    }

    @GetMapping
    @Operation(
            summary = "Получить профиль пассажира",
            description = "Возвращает данные профиля текущего пользователя",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Профиль найден"),
                    @ApiResponse(responseCode = "404", description = "Профиль не найден")
            }
    )
    public ResponseEntity<PassengerProfileResponse> getProfile(
            @RequestHeader("X-Account-ID") Long accountId
    ) {
        PassengerProfile profile = passengerProfileService.getProfile(accountId);

        return ResponseEntity.ok(PassengerProfileResponse.from(profile));
    }

    @PutMapping
    @Operation(
            summary = "Обновить профиль пассажира",
            description = "Обновляет имя, фамилию и/или фото",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "firstName": "Пётр",
                                      "lastName": "Петров",
                                      "photoUrl": "https://cdn.example.com/new-avatar.jpg"
                                    }
                                    """)
                    )
            ),
            responses = {
                    @ApiResponse(responseCode = "200", description = "Профиль обновлён"),
                    @ApiResponse(responseCode = "404", description = "Профиль не найден")
            }
    )
    public ResponseEntity<PassengerProfileResponse> updateProfile(
            @RequestHeader("X-Account-ID") Long accountId,
            @Valid @RequestBody UpdatePassengerProfileRequest request
    ) {
        PassengerProfile profile = passengerProfileService.updateProfile(
                accountId,
                request.firstName(),
                request.lastName(),
                request.photoUrl()
        );

        return ResponseEntity.ok(PassengerProfileResponse.from(profile));
    }
}

package com.example.userservice.controller;

import com.example.userservice.dto.request.UpdateLocationRequest;
import com.example.userservice.dto.response.DriverLocationResponse;
import com.example.userservice.entity.DriverLocation;
import com.example.userservice.service.DriverLocationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/drivers/location")
@RequiredArgsConstructor
@Tag(name = "Driver Location", description = "Управление геолокацией водителей (Будет заменено веб-сокетами)")
public class DriverLocationController {

    private final DriverLocationService driverLocationService;

    @PutMapping
    @Operation(
            summary = "Обновить локацию водителя",
            description = "Водитель отправляет координаты каждые 10 секунд (WebSocket или polling)",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "latitude": 55.7558260,
                                      "longitude": 37.6173040
                                    }
                                    """)
                    )
            ),
            responses = {
                    @ApiResponse(responseCode = "204", description = "Локация обновлена"),
                    @ApiResponse(responseCode = "404", description = "Профиль водителя не найден")
            }
    )
    public void updateLocation(
            @AuthenticationPrincipal Long driverId,
            @Valid @RequestBody UpdateLocationRequest request) {
        driverLocationService.updateLocation(
                driverId,
                request.latitude(),
                request.longitude()
        );
    }

    @GetMapping
    @Operation(
            summary = "Получить локацию водителя",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Локация найдена"),
                    @ApiResponse(responseCode = "404", description = "Локация не найдена")
            }
    )
    public DriverLocationResponse getLocation(@AuthenticationPrincipal Long driverId) {
        DriverLocation location = driverLocationService.getLocation(driverId);
        return DriverLocationResponse.from(location);
    }


    @GetMapping("/nearby")
    @Operation(
            summary = "Найти водителей поблизости",
            description = "Возвращает список ONLINE + верифицированных водителей в радиусе N км",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Список водителей")
            }
    )
    public List<DriverLocationResponse> findNearbyDrivers(
            @RequestParam BigDecimal latitude,
            @RequestParam BigDecimal longitude,
            @RequestParam(defaultValue = "5.0") BigDecimal radiusKm) {
        return driverLocationService.findNearbyDrivers(latitude, longitude, radiusKm).stream()
                .map(DriverLocationResponse::from)
                .toList();
    }
}

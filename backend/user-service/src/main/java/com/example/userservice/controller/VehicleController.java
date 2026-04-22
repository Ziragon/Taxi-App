package com.example.userservice.controller;

import com.example.userservice.dto.request.AddVehicleRequest;
import com.example.userservice.dto.request.UpdateVehicleRequest;
import com.example.userservice.dto.response.VehicleResponse;
import com.example.userservice.entity.Vehicle;
import com.example.userservice.service.VehicleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/vehicles")
@RequiredArgsConstructor
@Tag(name = "Vehicles", description = "Управление транспортом водителей")
public class VehicleController {

    private final VehicleService vehicleService;

    @PostMapping
    @Operation(
            summary = "Добавить автомобиль",
            description = "Водитель добавляет новый автомобиль. По умолчанию is_active = false",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "brand": "Toyota",
                                      "model": "Camry",
                                      "year": 2020,
                                      "color": "Черный",
                                      "licensePlate": "А123БВ777",
                                      "vehicleClass": "COMFORT"
                                    }
                                    """)
                    )
            ),
            responses = {
                    @ApiResponse(responseCode = "200", description = "Автомобиль добавлен"),
                    @ApiResponse(responseCode = "409", description = "Госномер уже существует")
            }
    )
    public ResponseEntity<VehicleResponse> addVehicle(
            @RequestHeader("X-Account-ID") Long driverId,
            @Valid @RequestBody AddVehicleRequest request
    ) {
        Vehicle vehicle = vehicleService.addVehicle(
                driverId,
                request.brand(),
                request.model(),
                request.year(),
                request.color(),
                request.licensePlate(),
                request.vehicleClass()
        );

        return ResponseEntity.ok(VehicleResponse.from(vehicle));
    }

    @GetMapping
    @Operation(
            summary = "Получить автомобили водителя",
            description = "Возвращает все автомобили текущего водителя",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Список автомобилей")
            }
    )
    public List<VehicleResponse> getVehicles(@RequestHeader("X-Account-ID") Long driverId) {
        return vehicleService.getVehiclesByDriver(driverId).stream()
                .map(VehicleResponse::from)
                .toList();
    }

    @PutMapping("/{vehicleId}")
    @Operation(
            summary = "Обновить данные автомобиля",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "brand": "Toyota",
                                      "model": "Corolla",
                                      "year": 2021,
                                      "color": "Белый",
                                      "licensePlate": "В456ГД777",
                                      "vehicleClass": "ECONOMY"
                                    }
                                    """)
                    )
            ),
            responses = {
                    @ApiResponse(responseCode = "200", description = "Автомобиль обновлён"),
                    @ApiResponse(responseCode = "404", description = "Автомобиль не найден")
            }
    )
    public ResponseEntity<VehicleResponse> updateVehicle(
            @PathVariable Long vehicleId,
            @Valid @RequestBody UpdateVehicleRequest request
    ) {
        Vehicle vehicle = vehicleService.updateVehicle(
                vehicleId,
                request.brand(),
                request.model(),
                request.year(),
                request.color(),
                request.licensePlate(),
                request.vehicleClass()
        );

        return ResponseEntity.ok(VehicleResponse.from(vehicle));
    }

    @PostMapping("/{vehicleId}/set-active")
    @Operation(
            summary = "Установить активный автомобиль",
            description = "Помечает выбранный автомобиль как активный, остальные становятся неактивными",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Автомобиль активирован")
            }
    )
    public ResponseEntity<Void> setActiveVehicle(
            @RequestHeader("X-Account-ID") Long driverId,
            @PathVariable Long vehicleId
    ) {
        vehicleService.setActiveVehicle(driverId, vehicleId);

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{vehicleId}")
    @Operation(
            summary = "Удалить автомобиль",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Автомобиль удалён"),
                    @ApiResponse(responseCode = "404", description = "Автомобиль не найден")
            }
    )
    public ResponseEntity<Void> deleteVehicle(
            @PathVariable Long vehicleId
    ) {
        vehicleService.deleteVehicle(vehicleId);

        return ResponseEntity.noContent().build();
    }
}

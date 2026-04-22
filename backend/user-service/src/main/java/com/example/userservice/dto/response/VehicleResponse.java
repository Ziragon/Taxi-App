package com.example.userservice.dto.response;

import com.example.userservice.entity.Vehicle;
import com.example.userservice.entity.enums.VehicleClass;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Данные транспорта")
public record VehicleResponse (

    @Schema(description = "ID транспорта", example = "789")
    Long id,

    @Schema(description = "ID водителя", example = "456")
    Long driverId,

    @Schema(description = "Марка", example = "Toyota")
    String brand,

    @Schema(description = "Модель", example = "Camry")
    String model,

    @Schema(description = "Год выпуска", example = "2020")
    Short year,

    @Schema(description = "Цвет", example = "Черный")
    String color,

    @Schema(description = "Госномер", example = "А123БВ777")
    String licensePlate,

    @Schema(description = "Класс", example = "COMFORT")
    VehicleClass vehicleClass,

    @Schema(description = "Активен ли", example = "true")
    Boolean isActive
) {
    public static VehicleResponse from(Vehicle vehicle) {
        return new VehicleResponse(
                vehicle.getId(),
                vehicle.getDriver().getAccountId(),
                vehicle.getBrand(),
                vehicle.getModel(),
                vehicle.getYear(),
                vehicle.getColor(),
                vehicle.getLicensePlate(),
                vehicle.getVehicleClass(),
                vehicle.isActive()
        );
    }
}

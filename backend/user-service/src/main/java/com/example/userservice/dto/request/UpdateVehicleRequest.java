package com.example.userservice.dto.request;

import com.example.userservice.entity.enums.VehicleClass;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@Schema(description = "Обновление данных транспорта")
public record UpdateVehicleRequest (

    @Schema(description = "Марка автомобиля", example = "Toyota")
    String brand,

    @Schema(description = "Модель автомобиля", example = "Corolla")
    String model,

    @Min(value = 1900, message = "Год не может быть меньше 1900")
    @Max(value = 2100, message = "Год не может быть больше 2100")
    @Schema(description = "Год выпуска", example = "2021")
    Short year,

    @Schema(description = "Цвет автомобиля", example = "Белый")
    String color,

    @Schema(description = "Государственный номер", example = "В456ГД777")
    String licensePlate,

    @Schema(description = "Класс автомобиля", example = "BUSINESS", allowableValues = {"ECONOMY", "COMFORT", "BUSINESS"})
    VehicleClass vehicleClass
) {}

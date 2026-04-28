package com.example.userservice.dto.request;

import com.example.shared.dto.enums.VehicleClass;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Добавление транспорта")
public record AddVehicleRequest (

    @NotBlank(message = "Марка обязательна")
    @Schema(description = "Марка автомобиля", example = "Toyota")
    String brand,

    @NotBlank(message = "Модель обязательна")
    @Schema(description = "Модель автомобиля", example = "Camry")
    String model,

    @NotNull(message = "Год выпуска обязателен")
    @Min(value = 1900, message = "Год не может быть меньше 1900")
    @Max(value = 2100, message = "Год не может быть больше 2100")
    @Schema(description = "Год выпуска", example = "2020")
    Short year,

    @NotBlank(message = "Цвет обязателен")
    @Schema(description = "Цвет автомобиля", example = "Черный")
    String color,

    @NotBlank(message = "Госномер обязателен")
    @Schema(description = "Государственный номер", example = "А123БВ777")
    String licensePlate,

    @NotNull(message = "Класс автомобиля обязателен")
    @Schema(description = "Класс автомобиля", example = "COMFORT", allowableValues = {"ECONOMY", "COMFORT", "BUSINESS"})
    VehicleClass vehicleClass
) {}

package com.example.userservice.dto.response;

import com.example.userservice.entity.enums.VehicleClass;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Данные транспорта")
public class VehicleResponse {

    @Schema(description = "ID транспорта", example = "789")
    private Long id;

    @Schema(description = "ID водителя", example = "456")
    private Long driverId;

    @Schema(description = "Марка", example = "Toyota")
    private String brand;

    @Schema(description = "Модель", example = "Camry")
    private String model;

    @Schema(description = "Год выпуска", example = "2020")
    private Short year;

    @Schema(description = "Цвет", example = "Черный")
    private String color;

    @Schema(description = "Госномер", example = "А123БВ777")
    private String licensePlate;

    @Schema(description = "Класс", example = "COMFORT")
    private VehicleClass vehicleClass;

    @Schema(description = "Активен ли", example = "true")
    private Boolean isActive;
}

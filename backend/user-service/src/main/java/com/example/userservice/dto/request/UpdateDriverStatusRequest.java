package com.example.userservice.dto.request;

import com.example.userservice.entity.enums.DriverStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Обновление статуса водителя")
public class UpdateDriverStatusRequest {

    @NotNull(message = "Статус обязателен")
    @Schema(description = "Новый статус", example = "ONLINE", allowableValues = {"ONLINE", "OFFLINE", "BUSY"})
    private DriverStatus status;
}

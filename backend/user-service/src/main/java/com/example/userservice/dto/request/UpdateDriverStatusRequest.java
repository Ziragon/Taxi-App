package com.example.userservice.dto.request;

import com.example.shared.dto.enums.DriverStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Обновление статуса водителя")
public record UpdateDriverStatusRequest (

    @NotNull(message = "Статус обязателен")
    @Schema(description = "Новый статус", example = "ONLINE", allowableValues = {"ONLINE", "OFFLINE", "BUSY"})
    DriverStatus status
) {}

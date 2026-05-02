package com.example.userservice.dto.response;

import com.example.shared.dto.enums.DriverStatus;
import io.swagger.v3.oas.annotations.media.Schema;

public record DriverStatusResponse(

        @Schema(description = "Статус", example = "ONLINE")
        DriverStatus status
) {}

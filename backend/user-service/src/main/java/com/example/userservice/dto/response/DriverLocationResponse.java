package com.example.userservice.dto.response;

import com.example.userservice.entity.DriverLocation;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;

@Schema(description = "Геолокация водителя")
public record DriverLocationResponse (

    @Schema(description = "ID водителя", example = "456")
    Long driverId,

    @Schema(description = "Широта", example = "55.7558260")
    BigDecimal latitude,

    @Schema(description = "Долгота", example = "37.6173040")
    BigDecimal longitude,

    @Schema(description = "Время последнего обновления", example = "2024-01-15T10:30:00Z")
    Instant updatedAt
) {
    public static DriverLocationResponse from(DriverLocation location) {
        return new DriverLocationResponse(
                location.getDriverId(),
                location.getLatitude(),
                location.getLongitude(),
                location.getUpdatedAt()
        );
    }
}

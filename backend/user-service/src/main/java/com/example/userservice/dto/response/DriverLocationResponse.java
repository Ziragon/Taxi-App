package com.example.userservice.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Геолокация водителя")
public class DriverLocationResponse {

    @Schema(description = "ID водителя", example = "456")
    private Long driverId;

    @Schema(description = "Широта", example = "55.7558260")
    private BigDecimal latitude;

    @Schema(description = "Долгота", example = "37.6173040")
    private BigDecimal longitude;

    @Schema(description = "Время последнего обновления", example = "2024-01-15T10:30:00Z")
    private Instant updatedAt;
}

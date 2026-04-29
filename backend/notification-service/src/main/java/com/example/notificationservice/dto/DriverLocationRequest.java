package com.example.notificationservice.dto;

import com.example.shared.dto.enums.VehicleClass;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record DriverLocationRequest(

        @NotNull
        BigDecimal longitude,

        @NotNull
        BigDecimal latitude,

        @NotNull
        VehicleClass vehicleClass
) {}

package com.example.userservice.dto.request;

import com.example.userservice.entity.enums.VehicleClass;

import java.math.BigDecimal;

public record DriverLocationRequest(

        BigDecimal longitude,

        BigDecimal latitude,

        VehicleClass vehicleClass
) {}

package com.example.shared.dto.request;

import com.example.shared.dto.enums.VehicleClass;

import java.math.BigDecimal;

public record NearbyDriversRequest(

        BigDecimal longitude,

        BigDecimal latitude,

        BigDecimal radius,

        VehicleClass vehicleClass
) {}

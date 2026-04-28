package com.example.userservice.dto.data;

import com.example.shared.dto.enums.VehicleClass;

public record DriverLocationDto(

        Long driverId,

        LocationDto location,

        VehicleClass vehicleClass
) {}

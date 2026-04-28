package com.example.userservice.dto.data;

import com.example.userservice.entity.enums.VehicleClass;

public record DriverLocationDto(

        Long driverId,

        LocationDto location,

        VehicleClass vehicleClass
) {}

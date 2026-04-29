package com.example.shared.dto.data;

import com.example.shared.dto.enums.VehicleClass;
import com.example.userservice.dto.data.LocationDto;

public record DriverLocationDto(

        Long driverId,

        LocationDto location,

        VehicleClass vehicleClass
) {}

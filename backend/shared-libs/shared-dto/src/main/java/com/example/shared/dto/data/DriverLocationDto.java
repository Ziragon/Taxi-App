package com.example.shared.dto.data;

import com.example.shared.dto.enums.VehicleClass;
import org.jspecify.annotations.Nullable;

public record DriverLocationDto(

        Long driverId,

        LocationDto location,

        @Nullable
        VehicleClass vehicleClass
) {}

package com.example.tripservice.dto.request;

import com.example.shared.dto.enums.VehicleClass;

public record TripSearchRequest(

        Long tripId,

        VehicleClass vehicleClass
) {}

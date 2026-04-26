package com.example.userservice.dto.data;

import com.example.userservice.entity.Vehicle;
import com.example.userservice.entity.enums.VehicleClass;

public record VehicleDto(
        Long id,

        Long driverId,

        String brand,

        String model,

        Short year,

        String color,

        String licensePlate,

        VehicleClass vehicleClass,

        boolean active
) {
    public static VehicleDto from(Vehicle vehicle) {
        return new VehicleDto(
                vehicle.getId(),
                vehicle.getDriver().getAccountId(),
                vehicle.getBrand(),
                vehicle.getModel(),
                vehicle.getYear(),
                vehicle.getColor(),
                vehicle.getLicensePlate(),
                vehicle.getVehicleClass(),
                vehicle.isActive()
        );
    }
}

package com.example.userservice.dto.data;

import com.example.shared.dto.enums.VehicleClass;
import com.example.userservice.entity.Vehicle;

public record VehicleDto(
        Long id,

        Long driverId,

        String brand,

        String model,

        Short year,

        String color,

        String licensePlate,

        VehicleClass vehicleClass,

        boolean active,

        boolean verified
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
                vehicle.isActive(),
                vehicle.isVerified()
        );
    }
}

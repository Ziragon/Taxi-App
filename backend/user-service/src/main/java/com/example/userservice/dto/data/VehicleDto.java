package com.example.userservice.dto.data;

import com.example.userservice.entity.Vehicle;

public record VehicleDto(
        Long id,

        String brand,

        String model,

        Short year,

        String color,

        String licensePlate,

        boolean active
) {
    public static VehicleDto from(Vehicle vehicle) {
        return new VehicleDto(
                vehicle.getId(),
                vehicle.getBrand(),
                vehicle.getModel(),
                vehicle.getYear(),
                vehicle.getColor(),
                vehicle.getLicensePlate(),
                vehicle.isActive()
        );
    }
}

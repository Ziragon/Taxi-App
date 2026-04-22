package com.example.userservice.service;

import com.example.userservice.entity.DriverProfile;
import com.example.userservice.entity.Vehicle;
import com.example.userservice.entity.enums.VehicleClass;
import com.example.userservice.exception.VehicleNotFoundException;
import com.example.userservice.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class VehicleService {

    private final VehicleRepository vehicleRepository;
    private final DriverProfileService driverProfileService;

    @Transactional
    public Vehicle addVehicle(Long driverId, String brand, String model, Short year,
                              String color, String licensePlate, VehicleClass vehicleClass) {
        DriverProfile driver = driverProfileService.getProfile(driverId);

        Vehicle vehicle = Vehicle.builder()
                .driver(driver)
                .brand(brand)
                .model(model)
                .year(year)
                .color(color)
                .licensePlate(licensePlate)
                .vehicleClass(vehicleClass)
                .active(false)
                .build();

        return vehicleRepository.save(vehicle);
    }

    @Transactional(readOnly = true)
    public Vehicle getVehicle(Long vehicleId) {
        return vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new VehicleNotFoundException(vehicleId));
    }

    @Transactional(readOnly = true)
    public List<Vehicle> getVehiclesByDriver(Long driverId) {
        return vehicleRepository.findAllByDriverAccountId(driverId);
    }

    @Transactional(readOnly = true)
    public List<Vehicle> getActiveVehiclesByDriver(Long driverId) {
        return vehicleRepository.findAllByDriverAccountIdAndActiveTrue(driverId);
    }

    @Transactional
    public Vehicle updateVehicle(Long vehicleId, String brand, String model, Short year,
                                 String color, String licensePlate, VehicleClass vehicleClass) {
        Vehicle vehicle = getVehicle(vehicleId);

        vehicle.setBrand(brand);
        vehicle.setModel(model);
        vehicle.setYear(year);
        vehicle.setColor(color);
        vehicle.setLicensePlate(licensePlate);
        vehicle.setVehicleClass(vehicleClass);

        return vehicleRepository.save(vehicle);
    }

    @Transactional
    public void setActiveVehicle(Long driverId, Long vehicleId) {
        List<Vehicle> driverVehicles = getVehiclesByDriver(driverId);

        driverVehicles.forEach(v -> {
            v.setActive(v.getId().equals(vehicleId));
            vehicleRepository.save(v);
        });
    }

    @Transactional
    public void deleteVehicle(Long vehicleId) {
        vehicleRepository.deleteById(vehicleId);
    }
}

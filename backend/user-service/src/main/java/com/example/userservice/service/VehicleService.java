package com.example.userservice.service;

import com.example.userservice.dto.data.VehicleDto;
import com.example.userservice.entity.DriverProfile;
import com.example.userservice.entity.Vehicle;
import com.example.userservice.entity.enums.VehicleClass;
import com.example.userservice.exception.ProfileNotFoundException;
import com.example.userservice.exception.VehicleNotFoundException;
import com.example.userservice.repository.DriverProfileRepository;
import com.example.userservice.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class VehicleService {

    private final VehicleRepository vehicleRepository;
    private final DriverProfileRepository driverProfileRepository;

    @Transactional
    public VehicleDto addVehicle(Long driverId, String brand, String model, Short year,
                              String color, String licensePlate, VehicleClass vehicleClass) {
        DriverProfile profile = driverProfileRepository.findById(driverId)
                .orElseThrow(() -> new ProfileNotFoundException("Driver", driverId));

        Vehicle vehicle = Vehicle.builder()
                .driver(profile)
                .brand(brand)
                .model(model)
                .year(year)
                .color(color)
                .licensePlate(licensePlate)
                .vehicleClass(vehicleClass)
                .active(false)
                .build();

        Vehicle saved = vehicleRepository.save(vehicle);
        return VehicleDto.from(saved);
    }

    @Transactional(readOnly = true)
    public VehicleDto getVehicle(Long vehicleId) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new VehicleNotFoundException(vehicleId));

        return VehicleDto.from(vehicle);
    }

    @Transactional(readOnly = true)
    public List<VehicleDto> getVehiclesByDriver(Long driverId) {
        List<Vehicle> vehicles = vehicleRepository.findAllByDriverAccountId(driverId);

        return vehicles.stream()
                .map(VehicleDto::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<VehicleDto> getActiveVehiclesByDriver(Long driverId) {
        List<Vehicle> vehicles = vehicleRepository.findAllByDriverAccountIdAndActiveTrue(driverId);

        return vehicles.stream()
                .map(VehicleDto::from)
                .toList();
    }

    @Transactional
    public VehicleDto updateVehicle(Long vehicleId, String brand, String model, Short year,
                                    String color, String licensePlate, VehicleClass vehicleClass) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new VehicleNotFoundException(vehicleId));

        vehicle.setBrand(brand);
        vehicle.setModel(model);
        vehicle.setYear(year);
        vehicle.setColor(color);
        vehicle.setLicensePlate(licensePlate);
        vehicle.setVehicleClass(vehicleClass);

        Vehicle saved = vehicleRepository.save(vehicle);
        return VehicleDto.from(saved);
    }

    @Transactional
    public void setActiveVehicle(Long driverId, Long vehicleId) {
        List<Vehicle> driverVehicles = vehicleRepository.findAllByDriverAccountId(driverId);

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

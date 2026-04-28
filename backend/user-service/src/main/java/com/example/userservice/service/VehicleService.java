package com.example.userservice.service;

import com.example.shared.dto.enums.VehicleClass;
import com.example.shared.exception.common.AccessDeniedException;
import com.example.shared.exception.common.ResourceNotFoundException;
import com.example.userservice.dto.data.VehicleDto;
import com.example.userservice.entity.DriverProfile;
import com.example.userservice.entity.Vehicle;
import com.example.userservice.entity.enums.DriverStatus;
import com.example.userservice.exception.VehicleAlreadyExistsException;
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
    private final DriverProfileService driverProfileService;

    @Transactional
    public VehicleDto addVehicle(Long driverId, String brand, String model, Short year,
                                 String color, String licensePlate, VehicleClass vehicleClass) {
        if (vehicleRepository.existsByLicensePlate(licensePlate)) {
            throw new VehicleAlreadyExistsException(licensePlate);
        }

        DriverProfile profile = driverProfileRepository.findById(driverId)
                .orElseThrow(() -> new ResourceNotFoundException("Driver profile", driverId));

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
    public List<VehicleDto> getVehiclesByDriver(Long driverId) {
        List<Vehicle> vehicles = vehicleRepository.findAllByDriverAccountId(driverId);

        return vehicles.stream()
                .map(VehicleDto::from)
                .toList();
    }

    @Transactional
    public VehicleDto updateVehicle(Long requesterId, Long vehicleId, String brand, String model, Short year,
                                    String color, String licensePlate, VehicleClass vehicleClass) {
        Vehicle vehicle = vehicleRepository.findByIdWithDriver(vehicleId)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle", vehicleId));

        checkOwnership(vehicle, requesterId);

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

        boolean vehicleBelongsToDriver = driverVehicles.stream()
                .anyMatch(v -> v.getId().equals(vehicleId));

        if (!vehicleBelongsToDriver) {
            throw new AccessDeniedException("You do not have access to this vehicle");
        }

        driverVehicles.forEach(v -> {
            v.setActive(v.getId().equals(vehicleId));
            vehicleRepository.save(v);
        });

        vehicleRepository.flush();
    }

    @Transactional
    public void deleteVehicle(Long requesterId, Long vehicleId) {
        Vehicle vehicle = vehicleRepository.findByIdWithDriver(vehicleId)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle", vehicleId));

        checkOwnership(vehicle, requesterId);

        if (vehicle.isActive()) {
            driverProfileService.updateStatus(requesterId, DriverStatus.OFFLINE);
        }

        vehicleRepository.deleteById(vehicleId);
    }

    private void checkOwnership(Vehicle vehicle, Long requesterId) {
        if (!vehicle.getDriver().getAccount().getId().equals(requesterId)) {
            throw new AccessDeniedException("You do not have access to this vehicle");
        }
    }
}

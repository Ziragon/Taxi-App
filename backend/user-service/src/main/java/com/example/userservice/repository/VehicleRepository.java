package com.example.userservice.repository;

import com.example.userservice.entity.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, Long> {

    List<Vehicle> findAllByDriverAccountId(Long driverAccountId);

    List<Vehicle> findAllByDriverAccountIdAndActiveTrue(Long driverAccountId);

    Optional<Vehicle> findByIdAndDriverAccountId(Long id, Long driverAccountId);

    boolean existsByLicensePlate(String licensePlate);
}

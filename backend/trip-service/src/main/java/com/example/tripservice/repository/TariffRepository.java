package com.example.tripservice.repository;

import com.example.shared.dto.enums.VehicleClass;
import com.example.tripservice.entity.Tariff;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TariffRepository extends JpaRepository<Tariff, Long> {
    List<Tariff> findAllByActive(boolean active);

    Optional<Tariff> findByTripClass(VehicleClass tripClass);
}

package com.example.userservice.repository;

import com.example.userservice.entity.Vehicle;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, Long> {

    List<Vehicle> findAllByDriverAccountId(Long driverAccountId);

    List<Vehicle> findAllByDriverAccountIdAndActiveTrue(Long driverAccountId);

    @Query("""
        SELECT v FROM Vehicle v
        JOIN FETCH v.driver d
        JOIN FETCH d.account
        WHERE v.id = :id
        """)
    Optional<Vehicle> findByIdWithDriver(@Param("id") Long id);

    boolean existsByLicensePlate(String licensePlate);

    @Query("SELECT v FROM Vehicle v JOIN FETCH v.driver d JOIN FETCH d.account")
    List<Vehicle> findAllWithDriver();
}

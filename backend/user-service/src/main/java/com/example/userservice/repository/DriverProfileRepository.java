package com.example.userservice.repository;

import com.example.userservice.entity.DriverProfile;
import com.example.shared.dto.enums.DriverStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DriverProfileRepository extends JpaRepository<DriverProfile, Long> {

    boolean existsByLicenseNumber(String licenseNumber);

    @Query("SELECT dp FROM DriverProfile dp JOIN FETCH dp.account")
    List<DriverProfile> findAllWithAccount();

    @Query("SELECT p FROM DriverProfile p JOIN FETCH p.account WHERE p.accountId = :id")
    Optional<DriverProfile> findByIdWithAccount(@Param("id") Long id);
}

package com.example.userservice.repository;

import com.example.userservice.entity.DriverProfile;
import com.example.userservice.entity.enums.DriverStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DriverProfileRepository extends JpaRepository<DriverProfile, Long> {

    List<DriverProfile> findAllByStatus(DriverStatus status);

    List<DriverProfile> findAllByVerifiedTrue();

    boolean existsByLicenseNumber(String licenseNumber);

    @Modifying
    @Query("UPDATE DriverProfile dp SET dp.status = :status WHERE dp.accountId = :accountId")
    int updateStatus(@Param("accountId") Long accountId, @Param("status") DriverStatus status);

    @Query("SELECT dp FROM DriverProfile dp JOIN FETCH dp.account")
    List<DriverProfile> findAllWithAccount();
}

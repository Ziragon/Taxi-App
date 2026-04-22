package com.example.userservice.repository;

import com.example.userservice.entity.DriverLocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DriverLocationRepository extends JpaRepository<DriverLocation, Long> {

    @Query("""
            SELECT dl FROM DriverLocation dl
            JOIN dl.driver dp
            WHERE dp.status = 'ONLINE'
              AND dp.isVerified = true
              AND ABS(dl.latitude - :lat) < :radius
              AND ABS(dl.longitude - :lng) < :radius
            """)
    List<DriverLocation> findNearbyOnlineDrivers(@Param("lat") java.math.BigDecimal lat,
                                                 @Param("lng") java.math.BigDecimal lng,
                                                 @Param("radius") java.math.BigDecimal radius);
}

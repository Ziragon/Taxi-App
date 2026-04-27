package com.example.userservice.service;

import com.example.userservice.entity.DriverLocation;
import com.example.userservice.entity.DriverProfile;
import com.example.userservice.exception.ProfileNotFoundException;
import com.example.userservice.repository.DriverLocationRepository;
import com.example.userservice.repository.DriverProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DriverLocationService {

    private final DriverLocationRepository driverLocationRepository;
    private final DriverProfileRepository driverProfileRepository;

    @Transactional
    public void updateLocation(Long driverId, BigDecimal latitude, BigDecimal longitude) {
        DriverProfile profile = driverProfileRepository.findById(driverId)
                .orElseThrow(() -> new ProfileNotFoundException("Driver", driverId));

        DriverLocation location = driverLocationRepository.findById(driverId)
                .orElse(DriverLocation.builder()
                        .driver(profile)
                        .build());

        location.setLatitude(latitude);
        location.setLongitude(longitude);

        driverLocationRepository.save(location);
    }

    @Transactional(readOnly = true)
    public DriverLocation getLocation(Long driverId) {
        return driverLocationRepository.findById(driverId)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public List<DriverLocation> findNearbyDrivers(BigDecimal lat, BigDecimal lng, BigDecimal radiusKm) {
        BigDecimal radiusDegrees = radiusKm.divide(BigDecimal.valueOf(111), 7, RoundingMode.HALF_UP);
        return driverLocationRepository.findNearbyOnlineDrivers(lat, lng, radiusDegrees);
    }
}

package com.example.userservice.service;

import com.example.shared.dto.enums.VehicleClass;
import com.example.shared.dto.data.DriverLocationDto;
import com.example.shared.dto.data.LocationDto;
import com.example.userservice.entity.DriverLocation;
import com.example.userservice.repository.DriverLocationRepository;
import com.example.userservice.repository.DriverProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class DriverLocationService {

    private final DriverCachingService driverCachingService;
    private final DriverLocationRepository locationRepository;
    private final DriverProfileRepository driverProfileRepository;

    public void updateLocation(Long driverId, BigDecimal longitude, BigDecimal latitude, VehicleClass vehicleClass) {
        DriverLocationDto dto = new DriverLocationDto(
                driverId,
                new LocationDto(longitude, latitude),
                vehicleClass
        );
        driverCachingService.updateLocation(dto);
    }

    public List<DriverLocationDto> getNearbyOnlineDrivers(
            BigDecimal lng, BigDecimal lat, BigDecimal radiusKm, VehicleClass vehicleClass) {
        return driverCachingService.getNearbyOnlineDrivers(
                lng.doubleValue(), lat.doubleValue(), radiusKm.doubleValue(), vehicleClass
        );
    }

    @Scheduled(fixedRate = 60_000)
    @Transactional
    public void persistLocationsToDatabase() {
        Instant recordedAt = Instant.now();

        Set<String> onlineIds = driverCachingService.getOnlineDriverIds();
        if (onlineIds == null || onlineIds.isEmpty()) return;

        List<DriverLocation> logs = driverCachingService
                .multiGetLocations(
                        onlineIds.stream()
                                .map(id -> "driver:location:" + id)
                                .toList()
                )
                .stream()
                .filter(Objects::nonNull)
                .map(dto -> DriverLocation.builder()
                        .driver(driverProfileRepository.getReferenceById(dto.driverId()))
                        .latitude(dto.location().latitude())
                        .longitude(dto.location().longitude())
                        .recordedAt(recordedAt)
                        .build())
                .toList();

        if (logs.isEmpty()) return;

        locationRepository.saveAll(logs);
        log.info("Logged locations for {} drivers at {}", logs.size(), recordedAt);
    }
}

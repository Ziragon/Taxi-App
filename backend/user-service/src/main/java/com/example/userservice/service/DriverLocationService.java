package com.example.userservice.service;

import com.example.userservice.dto.data.DriverLocationDto;
import com.example.userservice.dto.data.LocationDto;
import com.example.userservice.entity.enums.VehicleClass;
import com.example.userservice.repository.DriverLocationBatchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class DriverLocationService {

    private final DriverCachingService driverCachingService;
    private final DriverLocationBatchRepository batchRepository;

    public void updateLocation(Long driverId, BigDecimal longitude, BigDecimal latitude, VehicleClass vehicleClass) {
        DriverLocationDto dto = new DriverLocationDto(
                driverId,
                new LocationDto(longitude, latitude),
                vehicleClass
        );
        driverCachingService.updateLocation(dto);
    }

    public List<DriverLocationDto> getNearbyOnlineDrivers(
            double lng, double lat, double radiusKm, VehicleClass vehicleClass) {
        return driverCachingService.getNearbyOnlineDrivers(lng, lat, radiusKm, vehicleClass);
    }

    @Scheduled(fixedRate = 60_000)
    public void persistLocationsToDatabase() {
        Set<String> onlineIds = driverCachingService.getOnlineDriverIds();
        if (onlineIds == null || onlineIds.isEmpty()) return;

        List<DriverLocationDto> locations = driverCachingService
                .multiGetLocations(onlineIds.stream()
                        .map(id -> "driver:location:" + id)
                        .toList())
                .stream()
                .filter(Objects::nonNull)
                .toList();

        if (locations.isEmpty()) return;

        batchRepository.batchUpsert(locations);
        log.info("Persisted locations for {} drivers", locations.size());
    }
}

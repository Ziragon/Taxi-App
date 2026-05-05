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
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DriverLocationService {

    private final DriverCachingService driverCachingService;
    private final DriverLocationRepository locationRepository;
    private final DriverProfileRepository driverProfileRepository;

    private static final String KEY_LOCATION_PREFIX = "driver:location:";

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

        Set<String> onlineIds = driverCachingService.getOnlineDriverIds();
        if (onlineIds == null || onlineIds.isEmpty()) return;

        List<DriverLocation> data = getDataFromCache(onlineIds);

        Set<Long> driverIds = data.stream()
                .map(l -> l.getDriver().getAccountId())
                .collect(Collectors.toSet());

        Set<Long> existingIds = new HashSet<>(
                driverProfileRepository.findAllIdsByIdIn(driverIds)
        );

        List<DriverLocation> logs = data.stream() // Логируем только тех, кто есть в бд
                .filter(l -> existingIds.contains(l.getDriver().getAccountId()))
                .toList();

        if (logs.isEmpty()) return;

        locationRepository.saveAll(logs);
        log.info("Logged locations for {} drivers", logs.size());
    }

    private List<DriverLocation> getDataFromCache(Set<String> onlineIds) {
        Instant recordedAt = Instant.now();

        // Все локации активных водителей
        return driverCachingService
                .multiGetLocations(
                        onlineIds.stream()
                                .map(id -> KEY_LOCATION_PREFIX + id) // По key в Redis
                                .toList()
                )
                .stream()
                .filter(Objects::nonNull) // Not null
                .map(dto -> DriverLocation.builder()
                        .driver(driverProfileRepository.getReferenceById(dto.driverId())) // По Id водителей в БД
                        .latitude(dto.location().latitude()) // Координаты
                        .longitude(dto.location().longitude())
                        .vehicleClass(dto.vehicleClass()) // Класс машины
                        .recordedAt(recordedAt)
                        .build())
                .toList();
    }
}

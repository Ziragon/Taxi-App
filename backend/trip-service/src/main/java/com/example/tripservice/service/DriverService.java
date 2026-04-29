package com.example.tripservice.service;

import com.example.shared.dto.data.DriverLocationDto;
import com.example.shared.dto.enums.VehicleClass;
import com.example.shared.exception.common.ServiceUnavailableException;
import com.example.tripservice.client.DriverLocationClient;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DriverService {

    private final DriverLocationClient locationClient;

    public List<DriverLocationDto> getNearbyDrivers(BigDecimal longitude, BigDecimal latitude, BigDecimal radius) {
        try {
            return locationClient.getNearbyDrivers(longitude, latitude, radius, null);
        } catch (FeignException e) {
            log.error("Failed to fetch nearby drivers: {}", e.getMessage());
            throw new ServiceUnavailableException("User-service");
        }
    }

    public List<DriverLocationDto> getNearbyDrivers(BigDecimal longitude, BigDecimal latitude, BigDecimal radius, VehicleClass vehicleClass) {
        try {
            return locationClient.getNearbyDrivers(longitude, latitude, radius, vehicleClass);
        } catch (FeignException e) {
            log.error("Failed to fetch nearby drivers with class: {}", e.getMessage());
            throw new ServiceUnavailableException("User-service");
        }
    }
}

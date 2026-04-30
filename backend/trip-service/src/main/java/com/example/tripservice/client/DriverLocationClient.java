package com.example.tripservice.client;

import com.example.shared.dto.data.DriverLocationDto;
import com.example.shared.dto.enums.VehicleClass;
import com.example.shared.security.InternalFeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.util.List;

@FeignClient(
        name = "driver",
        url = "${internal.user-service}",
        configuration = InternalFeignConfig.class
)
public interface DriverLocationClient {

    @GetMapping("/api/v1/internal/drivers/nearby")
    List<DriverLocationDto> getNearbyDrivers(
            @RequestParam("lng") BigDecimal longitude,
            @RequestParam("lat") BigDecimal latitude,
            @RequestParam("rad") BigDecimal radius,
            @RequestParam("vehicleClass") VehicleClass vehicleClass
    );
}

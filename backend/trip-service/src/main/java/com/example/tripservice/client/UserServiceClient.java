package com.example.tripservice.client;

import com.example.shared.dto.data.DriverLocationDto;
import com.example.shared.dto.enums.VehicleClass;
import com.example.shared.dto.enums.DriverStatus;
import com.example.shared.security.InternalFeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.util.List;

@FeignClient(
        name = "driver",
        url = "${internal.user-service}",
        configuration = InternalFeignConfig.class
)
public interface UserServiceClient {

    @GetMapping("/api/v1/internal/drivers/nearby")
    List<DriverLocationDto> getNearbyDrivers(
            @RequestParam("lng") BigDecimal longitude,
            @RequestParam("lat") BigDecimal latitude,
            @RequestParam("rad") BigDecimal radius,
            @RequestParam("vehicleClass") VehicleClass vehicleClass
    );

    @GetMapping("/api/v1/internal/passenger/{accountId}/exists")
    Boolean hasPassengerProfile(@PathVariable Long accountId);

    @GetMapping("/api/v1/internal/driver/{accountId}/status")
    DriverStatus getDriverStatus(@PathVariable Long accountId);
}

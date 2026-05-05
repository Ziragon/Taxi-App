package com.example.userservice.client;

import com.example.shared.security.InternalFeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(
        name = "driver",
        url = "${internal.trip-service}",
        configuration = InternalFeignConfig.class
)
public interface TripClient {

    @GetMapping("/api/v1/internal/driver/{driverId}/trip-status")
    Boolean hasActiveTrip(@PathVariable Long driverId);
}

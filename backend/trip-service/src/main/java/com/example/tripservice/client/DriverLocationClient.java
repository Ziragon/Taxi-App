package com.example.tripservice.client;

import com.example.shared.dto.data.DriverLocationDto;
import com.example.shared.dto.request.NearbyDriversRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient(name = "driver", url = "${internal.user-service}")
public interface DriverLocationClient {

    @GetMapping("/api/v1/internal/drivers/nearby")
    List<DriverLocationDto> getNearbyDrivers(@RequestBody NearbyDriversRequest request);
}

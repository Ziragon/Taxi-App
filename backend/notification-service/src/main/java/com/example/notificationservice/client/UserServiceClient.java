package com.example.notificationservice.client;

import com.example.notificationservice.config.FeignConfig;
import com.example.notificationservice.dto.DriverLocationRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(
        name = "user-service",
        url = "${notification.internal.user-service-url}",
        configuration = FeignConfig.class
)
public interface UserServiceClient {

    @PutMapping("/api/v1/drivers/internal/{driverId}/location")
    void updateDriverLocation(
            @PathVariable Long driverId,
            @RequestBody DriverLocationRequest request
    );
}

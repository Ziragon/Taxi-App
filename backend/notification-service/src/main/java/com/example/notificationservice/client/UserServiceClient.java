package com.example.notificationservice.client;

import com.example.notificationservice.dto.DriverLocationRequest;
import com.example.notificationservice.dto.DriverProfileSnapshot;
import com.example.notificationservice.dto.PassengerProfileSnapshot;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(
        name = "user-service",
        url = "${notification.internal.user-service-url}"
)
public interface UserServiceClient {

    @PutMapping("/api/v1/internal/drivers/{driverId}/location")
    void updateDriverLocation(
            @PathVariable Long driverId,
            @RequestBody DriverLocationRequest request
    );

    @PutMapping("/api/v1/internal/driver/{driverId}/status/offline")
    void setDriverOffline(@PathVariable Long driverId);

    @PutMapping("/api/v1/internal/driver/{driverId}/status/busy")
    void setDriverBusy(@PathVariable Long driverId);

    @GetMapping("/api/v1/internal/driver/{driverId}/profile")
    DriverProfileSnapshot getDriverProfile(@PathVariable Long driverId);

    @GetMapping("/api/v1/internal/passenger/{passengerId}/profile")
    PassengerProfileSnapshot getPassengerProfile(@PathVariable Long passengerId);

}

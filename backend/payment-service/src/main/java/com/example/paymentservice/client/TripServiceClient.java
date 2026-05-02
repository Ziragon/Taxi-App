package com.example.paymentservice.client;

import com.example.paymentservice.dto.response.TripResponse;
import com.example.paymentservice.config.FeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(
        name = "trip-service",
        url = "${internal.trip-service}",
        configuration = FeignConfig.class
)
public interface TripServiceClient {

    @GetMapping("/api/v1/internal/trips/{tripId}")
    TripResponse getTripById(@PathVariable Long tripId);
}

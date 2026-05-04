package com.example.userservice.service;

import com.example.userservice.client.TripClient;
import com.example.userservice.exception.DriverStatusValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class TripStatusService {

    private final TripClient tripClient;

    public void validateDriverStatus(Long driverId) {
        boolean status = tripClient.hasActiveTrip(driverId);
        if(status) {
            throw new DriverStatusValidationException("Driver has an active trip");
        }
    }
}

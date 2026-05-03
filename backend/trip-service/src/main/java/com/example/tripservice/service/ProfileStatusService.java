package com.example.tripservice.service;

import com.example.shared.exception.common.AccessDeniedException;
import com.example.shared.dto.enums.DriverStatus;
import com.example.tripservice.client.UserServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProfileStatusService {

    private final UserServiceClient userClient;

    public void verifyPassengerCanOrder(Long accountId) {
        Boolean status = userClient.hasPassengerProfile(accountId);
        if (!Boolean.TRUE.equals(status)) {
            throw new AccessDeniedException("Passenger profile not found");
        }
    }

    public void verifyDriverCanDrive(Long accountId) {
        DriverStatus status = userClient.getDriverStatus(accountId);
        if (!DriverStatus.ONLINE.equals(status)) {
            throw new AccessDeniedException("Driver is not ONLINE");
        }
    }

    public void setDriverStatusBusy(Long driverId) {
        userClient.setBusyStatus(driverId);
    }
}

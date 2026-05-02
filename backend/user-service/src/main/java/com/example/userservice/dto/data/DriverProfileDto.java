package com.example.userservice.dto.data;

import com.example.userservice.entity.DriverProfile;
import com.example.shared.dto.enums.DriverStatus;

import java.math.BigDecimal;

public record DriverProfileDto(

        Long accountId,

        String firstName,

        String lastName,

        String photoUrl,

        String licenseNumber,

        DriverStatus status,

        BigDecimal averageRating,

        Integer totalTrips,

        boolean verified
) {
    public static DriverProfileDto from(DriverProfile profile) {
        return new DriverProfileDto(
                profile.getAccountId(),
                profile.getFirstName(),
                profile.getLastName(),
                profile.getPhotoUrl(),
                profile.getLicenseNumber(),
                profile.getStatus(),
                profile.getAverageRating(),
                profile.getTotalTrips(),
                profile.isVerified()
        );
    }
}

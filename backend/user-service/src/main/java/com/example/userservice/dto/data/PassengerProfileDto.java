package com.example.userservice.dto.data;

import com.example.userservice.entity.PassengerProfile;

import java.math.BigDecimal;

public record PassengerProfileDto(

        Long accountId,

        String firstName,

        String lastName,

        String photoUrl,

        BigDecimal averageRating,

        Integer totalTrips
) {
     public static PassengerProfileDto from(PassengerProfile profile) {
         return new PassengerProfileDto(
                 profile.getAccountId(),
                 profile.getFirstName(),
                 profile.getLastName(),
                 profile.getPhotoUrl(),
                 profile.getAverageRating(),
                 profile.getTotalTrips()
         );
     }
}
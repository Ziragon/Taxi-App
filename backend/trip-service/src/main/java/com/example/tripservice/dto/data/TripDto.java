package com.example.tripservice.dto.data;

import com.example.tripservice.entity.Trip;

public record TripDto(

) {
    public static TripDto from(Trip trip) {
        return new TripDto();
    }
}

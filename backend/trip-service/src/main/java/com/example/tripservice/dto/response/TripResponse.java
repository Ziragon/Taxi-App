package com.example.tripservice.dto.response;

import com.example.tripservice.dto.data.TripDto;

public record TripResponse(

) {
    public static TripResponse from(TripDto result) {
        return new TripResponse();
    }
}

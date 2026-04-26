package com.example.tripservice.dto.data;

import com.example.tripservice.dto.request.TripCreateRequest;

import java.math.BigDecimal;

public record TripCreateDto(
        String originAddress,

        BigDecimal originLat,

        BigDecimal originLng,

        String destAddress,

        BigDecimal destLat,

        BigDecimal destLng
) {
    public static TripCreateDto from(TripCreateRequest request) {
        return new TripCreateDto(
                request.originAddress(),
                request.originLat(),
                request.originLng(),
                request.destAddress(),
                request.destLat(),
                request.destLng()
        );
    }
}

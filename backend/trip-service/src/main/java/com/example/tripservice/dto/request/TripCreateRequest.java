package com.example.tripservice.dto.request;

import java.math.BigDecimal;

public record TripCreateRequest(

        String originAddress,

        BigDecimal originLat,

        BigDecimal originLng,

        String destAddress,

        BigDecimal destLat,

        BigDecimal destLng
) {}

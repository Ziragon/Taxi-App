package com.example.tripservice.dto.response;

import java.math.BigDecimal;

public record DriverCoordinatesRequest(

        BigDecimal latitude,

        BigDecimal longitude
) {}

package com.example.userservice.dto.data;

import java.math.BigDecimal;

public record DriverLocationDto(

        Long driverId,

        BigDecimal longitude,

        BigDecimal latitude
) {}

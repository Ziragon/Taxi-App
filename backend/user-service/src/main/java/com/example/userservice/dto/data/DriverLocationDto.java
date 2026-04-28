package com.example.userservice.dto.data;

import java.math.BigDecimal;

public record DriverLocationDto(

        BigDecimal longitude,

        BigDecimal latitude
) {}

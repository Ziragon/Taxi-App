package com.example.userservice.dto.data;

import java.math.BigDecimal;

public record LocationDto(

        BigDecimal longitude,

        BigDecimal latitude
) {}

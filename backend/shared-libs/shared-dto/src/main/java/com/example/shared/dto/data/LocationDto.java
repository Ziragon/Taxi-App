package com.example.shared.dto.data;

import java.math.BigDecimal;

public record LocationDto(

        BigDecimal longitude,

        BigDecimal latitude
) {}

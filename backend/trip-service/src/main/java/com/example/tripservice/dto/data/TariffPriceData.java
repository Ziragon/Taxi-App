package com.example.tripservice.dto.data;

import java.math.BigDecimal;

public record TariffPriceData(

        BigDecimal distanceCost,

        BigDecimal timeCost,

        BigDecimal price
) {}

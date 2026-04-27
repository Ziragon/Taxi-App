package com.example.tripservice.dto.data;

import java.math.BigDecimal;

public record PriceBreakdown (

        BigDecimal baseFare,

        BigDecimal kmCost,

        BigDecimal minCost,

        BigDecimal weatherCoef,

        BigDecimal surgeCoef
) {}

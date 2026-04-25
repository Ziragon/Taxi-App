package com.example.tripservice.dto;

import java.math.BigDecimal;

public record PriceBreakdown (

        BigDecimal baseFare,

        BigDecimal kmCost,

        BigDecimal minCost,

        BigDecimal weatherCoef,

        BigDecimal surgeCoef
) {}

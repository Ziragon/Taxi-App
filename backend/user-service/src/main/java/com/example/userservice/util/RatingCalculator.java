package com.example.userservice.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class RatingCalculator {

    private RatingCalculator() {}

    public static BigDecimal calculate(BigDecimal currentAverage, int currentTotal, BigDecimal newRating) {
        int newTotal = currentTotal + 1;
        return currentAverage
                .multiply(BigDecimal.valueOf(currentTotal))
                .add(newRating)
                .divide(BigDecimal.valueOf(newTotal), 2, RoundingMode.HALF_UP);
    }
}

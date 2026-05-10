package com.example.tripservice.dto.request;

import com.example.tripservice.entity.enums.AccountType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record RatingRequest(

        Long tripId,

        Long rateeId,

        AccountType accountType,

        @Min(1) @Max(5)
        Integer score,

        String comment
) {}

package com.example.tripservice.dto.client;

import java.math.BigDecimal;

public record CreateRefundRequest(
        Long transactionId,
        BigDecimal amount,
        String reason
) {}

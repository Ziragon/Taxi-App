package com.example.paymentservice.exception;

import com.example.shared.exception.base.BaseException;
import com.example.shared.exception.base.ErrorType;

public class DuplicatePaymentMethodException extends BaseException {

    public DuplicatePaymentMethodException(Long passengerId, String cardLast4) {
        super("Payment method already exists for passenger %s with card ending in %s".formatted(passengerId, cardLast4),
                ErrorType.CONFLICT);
    }

    public DuplicatePaymentMethodException(String message) {
        super(message, ErrorType.CONFLICT);
    }
}

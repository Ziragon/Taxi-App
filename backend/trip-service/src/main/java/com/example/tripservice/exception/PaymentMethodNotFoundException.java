package com.example.tripservice.exception;

import com.example.shared.exception.base.BaseException;
import com.example.shared.exception.base.ErrorType;

public class PaymentMethodNotFoundException extends BaseException {

    public PaymentMethodNotFoundException(Long id) {
        super("Payment method not found with id: %s".formatted(id), ErrorType.NOT_FOUND);
    }

    public PaymentMethodNotFoundException(String message) {
        super(message, ErrorType.NOT_FOUND);
    }
}


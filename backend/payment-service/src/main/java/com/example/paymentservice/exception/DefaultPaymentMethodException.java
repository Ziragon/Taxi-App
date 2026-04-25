package com.example.paymentservice.exception;

import com.example.shared.exception.base.BaseException;
import com.example.shared.exception.base.ErrorType;

public class DefaultPaymentMethodException extends BaseException {

    public DefaultPaymentMethodException(String message) {
        super(message, ErrorType.BAD_REQUEST);
    }

    public DefaultPaymentMethodException(Long passengerId) {
        super("Cannot delete default payment method for passenger: %s".formatted(passengerId), ErrorType.BAD_REQUEST);
    }
}

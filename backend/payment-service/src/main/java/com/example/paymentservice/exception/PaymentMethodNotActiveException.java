package com.example.paymentservice.exception;

import com.example.shared.exception.base.BaseException;
import com.example.shared.exception.base.ErrorType;

public class PaymentMethodNotActiveException extends BaseException {

    public PaymentMethodNotActiveException(Long paymentMethodId) {
        super("Payment method is not active: %s".formatted(paymentMethodId), ErrorType.BAD_REQUEST);
    }

    public PaymentMethodNotActiveException(String message) {
        super(message, ErrorType.BAD_REQUEST);
    }
}

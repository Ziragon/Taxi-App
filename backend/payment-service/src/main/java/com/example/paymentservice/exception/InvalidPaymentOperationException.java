package com.example.paymentservice.exception;

import com.example.shared.exception.base.BaseException;
import com.example.shared.exception.base.ErrorType;

public class InvalidPaymentOperationException extends BaseException {

    public InvalidPaymentOperationException(String message) {
        super(message, ErrorType.BAD_REQUEST);
    }

    public InvalidPaymentOperationException(String resource, String reason) {
        super("Invalid operation on %s: %s".formatted(resource, reason), ErrorType.BAD_REQUEST);
    }
}

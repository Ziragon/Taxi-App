package com.example.paymentservice.exception;

import com.example.shared.exception.base.BaseException;
import com.example.shared.exception.base.ErrorType;

public class PaymentProcessingException extends BaseException {

    public PaymentProcessingException(String message) {
        super(message, ErrorType.INTERNAL);
    }

    public PaymentProcessingException(String message, Throwable cause) {
        super(message, ErrorType.INTERNAL, cause);
    }
}

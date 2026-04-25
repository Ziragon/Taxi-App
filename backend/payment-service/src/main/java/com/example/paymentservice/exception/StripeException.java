package com.example.paymentservice.exception;

import com.example.shared.exception.base.BaseException;
import com.example.shared.exception.base.ErrorType;

public class StripeException extends BaseException {

    public StripeException(String message) {
        super(message, ErrorType.INTERNAL);
    }

    public StripeException(String message, Throwable cause) {
        super(message, ErrorType.INTERNAL, cause);
    }

    public StripeException(String message, int statusCode) {
        super(message, ErrorType.INTERNAL, statusCode);
    }
}

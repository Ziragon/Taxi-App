package com.example.paymentservice.exception;

import com.example.shared.exception.base.BaseException;
import com.example.shared.exception.base.ErrorType;

public class PayoutAccountNotVerifiedException extends BaseException {

    public PayoutAccountNotVerifiedException(Long accountId) {
        super("Payout account not verified for driver: %s".formatted(accountId), ErrorType.VALIDATION);
    }

    public PayoutAccountNotVerifiedException(String message) {
        super(message, ErrorType.VALIDATION);
    }
}
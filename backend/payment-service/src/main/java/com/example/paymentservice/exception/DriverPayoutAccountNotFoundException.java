package com.example.paymentservice.exception;

import com.example.shared.exception.base.BaseException;
import com.example.shared.exception.base.ErrorType;

public class DriverPayoutAccountNotFoundException extends BaseException {

    public DriverPayoutAccountNotFoundException(Long id) {
        super("Driver payout account not found with id: %s".formatted(id), ErrorType.NOT_FOUND);
    }

    public DriverPayoutAccountNotFoundException(String field, Object value) {
        super("Driver payout account not found with %s: %s".formatted(field, value), ErrorType.NOT_FOUND);
    }
}
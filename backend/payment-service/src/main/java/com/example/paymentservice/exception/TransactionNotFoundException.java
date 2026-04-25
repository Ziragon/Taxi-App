package com.example.paymentservice.exception;

import com.example.shared.exception.base.BaseException;
import com.example.shared.exception.base.ErrorType;

public class TransactionNotFoundException extends BaseException {

    public TransactionNotFoundException(Long id) {
        super("Transaction not found with id: %s".formatted(id), ErrorType.NOT_FOUND);
    }

    public TransactionNotFoundException(String field, Object value) {
        super("Transaction not found with %s: %s".formatted(field, value), ErrorType.NOT_FOUND);
    }
}

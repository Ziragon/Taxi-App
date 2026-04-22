package com.example.shared.exception.common;

import com.example.shared.exception.base.BaseException;
import com.example.shared.exception.base.ErrorType;

public class ValidationException extends BaseException {

    public ValidationException(String message) {
        super(message, ErrorType.VALIDATION);
    }

    public ValidationException(String field, String reason) {
        super("Validation failed for field '%s': %s".formatted(field, reason), ErrorType.VALIDATION);
    }
}
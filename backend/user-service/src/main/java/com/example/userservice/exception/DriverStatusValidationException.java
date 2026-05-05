package com.example.userservice.exception;

import com.example.shared.exception.common.ValidationException;

public class DriverStatusValidationException extends ValidationException {

    public DriverStatusValidationException(String message) {
        super(message);
    }
}
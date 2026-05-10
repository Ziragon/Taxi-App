package com.example.tripservice.exception;

import com.example.shared.exception.common.ValidationException;

public class TripNotCompletedException extends ValidationException {
    public TripNotCompletedException(String message) {
        super(message);
    }
}

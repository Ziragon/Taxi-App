package com.example.tripservice.exception;

import com.example.shared.exception.common.ValidationException;

public class TripBookingException extends ValidationException {
    public TripBookingException(String message) {
        super(message);
    }
}

package com.example.tripservice.exception;

import com.example.shared.exception.common.ResourceAlreadyExistsException;

public class TripAlreadyExistsException extends ResourceAlreadyExistsException {
    public TripAlreadyExistsException() {
        super("User already have an active trip");
    }
}

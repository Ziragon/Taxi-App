package com.example.tripservice.exception;

import com.example.shared.exception.common.ResourceNotFoundException;

public class TripNotFoundException extends ResourceNotFoundException {
    public TripNotFoundException(Long id) {
        super("Trip", id);
    }
}

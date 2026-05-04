package com.example.tripservice.exception;

import com.example.shared.exception.common.ValidationException;
import com.example.tripservice.entity.enums.TripStatus;

public class InvalidTripStatusException extends ValidationException {
    public InvalidTripStatusException(Long tripId, TripStatus expected, TripStatus status) {
        super("Invalid status operation for trip " + tripId + ": expected - " + expected + ", actual - " + status);
    }

    public InvalidTripStatusException(Long tripId, TripStatus status) {
        super("Invalid status operation for trip " + tripId + ": must not have a status - " + status);
    }
}

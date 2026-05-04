package com.example.tripservice.util;

import com.example.tripservice.entity.Trip;
import com.example.tripservice.entity.enums.TripStatus;
import com.example.tripservice.exception.InvalidTripStatusException;
import com.example.tripservice.exception.TripAlreadyExistsException;

import java.util.List;

public class StatusValidationUtil {

    private StatusValidationUtil() {}

    private static final List<TripStatus> ACTIVE_STATUSES = List.of(
            TripStatus.SEARCHING, TripStatus.DRIVER_ASSIGNED, TripStatus.IN_PROGRESS
    );

    public static void assertTripNotActive(Trip trip) {
        if (ACTIVE_STATUSES.contains(trip.getStatus())) {
            throw new TripAlreadyExistsException();
        }
    }

    public static void assertTripHasStatus(Trip trip, TripStatus expected) {
        if (trip.getStatus() != expected) {
            throw new InvalidTripStatusException(trip.getId(), expected, trip.getStatus());
        }
    }

    public static void assertTripStatusNotIn(Trip trip, List<TripStatus> forbiddenStatuses) {
        if (forbiddenStatuses.contains(trip.getStatus())) {
            throw new InvalidTripStatusException(
                    trip.getId(), trip.getStatus()
            );
        }
    }

    public static void assertTripHasNotStatus(Trip trip, TripStatus expected) {
        if (trip.getStatus() == expected) {
            throw new InvalidTripStatusException(trip.getId(), expected);
        }
    }
}

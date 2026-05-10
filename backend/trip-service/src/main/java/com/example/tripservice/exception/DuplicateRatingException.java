package com.example.tripservice.exception;

import com.example.shared.exception.common.ResourceAlreadyExistsException;

public class DuplicateRatingException extends ResourceAlreadyExistsException {
    public DuplicateRatingException() {
        super("User already rated this person");
    }
}

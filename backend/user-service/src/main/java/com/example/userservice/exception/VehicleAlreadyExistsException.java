package com.example.userservice.exception;

import com.example.shared.exception.base.BaseException;
import com.example.shared.exception.base.ErrorType;

public class VehicleAlreadyExistsException extends BaseException {

    public VehicleAlreadyExistsException(String licensePlate) {
        super("Vehicle with license plate %s already exists".formatted(licensePlate), ErrorType.CONFLICT);
    }
}

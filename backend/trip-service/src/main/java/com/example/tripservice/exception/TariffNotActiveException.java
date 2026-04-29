package com.example.tripservice.exception;

import com.example.shared.dto.enums.VehicleClass;
import com.example.shared.exception.common.ValidationException;

public class TariffNotActiveException extends ValidationException {
    public TariffNotActiveException(VehicleClass vehicle) {
        super(vehicle.name(), "not active");
    }
}

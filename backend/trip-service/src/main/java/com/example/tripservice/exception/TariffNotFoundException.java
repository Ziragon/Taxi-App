package com.example.tripservice.exception;

import com.example.shared.dto.enums.VehicleClass;
import com.example.shared.exception.common.ResourceNotFoundException;

public class TariffNotFoundException extends ResourceNotFoundException {
    public TariffNotFoundException(VehicleClass vehicle) {
        super("Tariff", "vehicle class", vehicle.name());
    }
}

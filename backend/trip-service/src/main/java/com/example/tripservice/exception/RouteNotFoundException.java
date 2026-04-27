package com.example.tripservice.exception;

import com.example.shared.exception.common.ResourceNotFoundException;

public class RouteNotFoundException extends ResourceNotFoundException {
    public RouteNotFoundException(String coords) {
        super("Route", "coords", coords);
    }
}

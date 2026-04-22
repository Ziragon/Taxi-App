package com.example.userservice.exception;

import com.example.shared.exception.common.ResourceNotFoundException;

public class VehicleNotFoundException extends ResourceNotFoundException {

  public VehicleNotFoundException(Long id) {
    super("Vehicle", id);
  }
}
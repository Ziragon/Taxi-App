package com.example.userservice.exception;

import com.example.shared.exception.base.BaseException;
import com.example.shared.exception.base.ErrorType;

public class ProfileAlreadyExistsException extends BaseException {

  public ProfileAlreadyExistsException(String profileType) {
    super("%s profile already exists".formatted(profileType), ErrorType.CONFLICT);
  }
}
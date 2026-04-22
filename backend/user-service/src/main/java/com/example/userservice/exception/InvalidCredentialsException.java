package com.example.userservice.exception;

import com.example.shared.exception.common.AuthenticationException;

public class InvalidCredentialsException extends AuthenticationException {

    public InvalidCredentialsException() {
        super("Invalid email or password");
    }
}
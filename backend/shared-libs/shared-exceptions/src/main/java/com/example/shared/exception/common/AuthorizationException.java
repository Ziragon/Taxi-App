package com.example.shared.exception.common;

import com.example.shared.exception.base.BaseException;
import com.example.shared.exception.base.ErrorType;

public class AuthorizationException extends BaseException {

    public AuthorizationException(String message) {
        super(message, ErrorType.AUTHORIZATION);
    }

    public AuthorizationException() {
        super("Access denied", ErrorType.AUTHORIZATION);
    }
}
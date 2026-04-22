package com.example.shared.exception.common;

import com.example.shared.exception.base.BaseException;
import com.example.shared.exception.base.ErrorType;

public class AuthenticationException extends BaseException {

    public AuthenticationException(String message) {
        super(message, ErrorType.AUTHENTICATION);
    }

    public AuthenticationException(String message, Throwable cause) {
        super(message, ErrorType.AUTHENTICATION, cause);
    }
}
package com.example.shared.exception.common;

import com.example.shared.exception.base.BaseException;
import com.example.shared.exception.base.ErrorType;

public class AccessDeniedException extends BaseException {

    public AccessDeniedException() {
        super("Access denied", ErrorType.AUTHORIZATION);
    }

    public AccessDeniedException(String message) {
        super(message, ErrorType.AUTHORIZATION);
    }
}

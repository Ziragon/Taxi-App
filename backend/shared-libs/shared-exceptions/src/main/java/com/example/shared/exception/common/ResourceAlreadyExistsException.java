package com.example.shared.exception.common;

import com.example.shared.exception.base.BaseException;
import com.example.shared.exception.base.ErrorType;

public class ResourceAlreadyExistsException extends BaseException {

    public ResourceAlreadyExistsException(String resource, String field, Object value) {
        super("%s already exists with %s: %s".formatted(resource, field, value), ErrorType.CONFLICT);
    }

    public ResourceAlreadyExistsException(String message) {
        super(message, ErrorType.CONFLICT);
    }
}
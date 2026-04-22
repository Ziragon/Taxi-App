package com.example.shared.exception.common;

import com.example.shared.exception.base.BaseException;
import com.example.shared.exception.base.ErrorType;

public class ResourceNotFoundException extends BaseException {

    public ResourceNotFoundException(String resource, Object id) {
        super("%s not found with id: %s".formatted(resource, id), ErrorType.NOT_FOUND);
    }

    public ResourceNotFoundException(String resource, String field, Object value) {
        super("%s not found with %s: %s".formatted(resource, field, value), ErrorType.NOT_FOUND);
    }

    public ResourceNotFoundException(String message) {
        super(message, ErrorType.NOT_FOUND);
    }
}
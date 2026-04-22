package com.example.shared.exception.common;

import com.example.shared.exception.base.BaseException;
import com.example.shared.exception.base.ErrorType;

public class ServiceUnavailableException extends BaseException {

    public ServiceUnavailableException(String service) {
        super("Service unavailable: %s".formatted(service), ErrorType.SERVICE_UNAVAILABLE);
    }

    public ServiceUnavailableException(String service, Throwable cause) {
        super("Service unavailable: %s".formatted(service), ErrorType.SERVICE_UNAVAILABLE, cause);
    }
}
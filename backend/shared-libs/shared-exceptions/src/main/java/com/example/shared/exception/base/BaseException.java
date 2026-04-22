package com.example.shared.exception.base;


import lombok.Getter;

@Getter
public abstract class BaseException extends RuntimeException {

    private final ErrorType errorType;
    private final int statusCode;

    protected BaseException(String message, ErrorType errorType) {
        super(message);
        this.errorType = errorType;
        this.statusCode = errorType.getDefaultStatus();
    }

    protected BaseException(String message, ErrorType errorType, int statusCode) {
        super(message);
        this.errorType = errorType;
        this.statusCode = statusCode;
    }

    protected BaseException(String message, ErrorType errorType, Throwable cause) {
        super(message, cause);
        this.errorType = errorType;
        this.statusCode = errorType.getDefaultStatus();
    }

}

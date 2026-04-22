package com.example.shared.exception.base;

import java.time.OffsetDateTime;
import java.util.Map;

public record ErrorResponse(
        int status,
        String error,
        String message,
        String path,
        OffsetDateTime timestamp,
        Map<String, String> details
) {

    public ErrorResponse(int status, String error, String message, String path) {
        this(status, error, message, path, OffsetDateTime.now(), null);
    }

    public ErrorResponse(int status, String error, String message, String path, Map<String, String> details) {
        this(status, error, message, path, OffsetDateTime.now(), details);
    }
}

package com.example.shared.exception.base;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
public abstract class AbstractExceptionHandler {

    protected ResponseEntity<ErrorResponse> handleBaseException(BaseException ex,
                                                                HttpServletRequest request) {
        log.warn("Business exception at {}: {}", request.getRequestURI(), ex.getMessage());

        var response = new ErrorResponse(
                ex.getStatusCode(),
                ex.getErrorType().name(),
                ex.getMessage(),
                request.getRequestURI()
        );

        return ResponseEntity.status(ex.getStatusCode()).body(response);
    }

    protected ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex,
                                                             HttpServletRequest request) {
        Map<String, String> details = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                details.put(error.getField(), error.getDefaultMessage())
        );

        log.warn("Validation failed at {}: {}", request.getRequestURI(), details);

        var response = new ErrorResponse(
                422,
                "VALIDATION",
                "Validation failed",
                request.getRequestURI(),
                details
        );

        return ResponseEntity.unprocessableContent().body(response);
    }

    protected ResponseEntity<ErrorResponse> handleUnreadable(HttpMessageNotReadableException ex,
                                                             HttpServletRequest request) {
        log.warn("Malformed request body at {}: {}", request.getRequestURI(), ex.getMessage());

        var response = new ErrorResponse(
                400,
                "BAD_REQUEST",
                "Malformed request body",
                request.getRequestURI()
        );

        return ResponseEntity.badRequest().body(response);
    }

    protected ResponseEntity<ErrorResponse> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex,
                                                                     HttpServletRequest request) {
        var response = new ErrorResponse(
                405,
                "METHOD_NOT_ALLOWED",
                "Method '%s' is not supported".formatted(ex.getMethod()),
                request.getRequestURI()
        );

        return ResponseEntity.status(405).body(response);
    }

    protected ResponseEntity<ErrorResponse> handleMediaType(HttpMediaTypeNotSupportedException ex,
                                                            HttpServletRequest request) {
        var response = new ErrorResponse(
                415,
                "UNSUPPORTED_MEDIA_TYPE",
                "Content type '%s' is not supported".formatted(ex.getContentType()),
                request.getRequestURI()
        );

        return ResponseEntity.status(415).body(response);
    }

    protected ResponseEntity<ErrorResponse> handleMissingParam(MissingServletRequestParameterException ex,
                                                               HttpServletRequest request) {
        var response = new ErrorResponse(
                400,
                "BAD_REQUEST",
                "Required parameter '%s' is missing".formatted(ex.getParameterName()),
                request.getRequestURI()
        );

        return ResponseEntity.badRequest().body(response);
    }

    protected ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex,
                                                               HttpServletRequest request) {
        var response = new ErrorResponse(
                400,
                "BAD_REQUEST",
                "Parameter '%s' must be of type '%s'".formatted(
                        ex.getName(),
                        ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown"
                ),
                request.getRequestURI()
        );

        return ResponseEntity.badRequest().body(response);
    }

    protected ResponseEntity<ErrorResponse> handleNoResource(NoResourceFoundException ex,
                                                             HttpServletRequest request) {
        var response = new ErrorResponse(
                404,
                "NOT_FOUND",
                "Resource not found",
                request.getRequestURI()
        );

        return ResponseEntity.status(404).body(response);
    }

    protected ResponseEntity<ErrorResponse> handleGeneric(Exception ex,
                                                          HttpServletRequest request) {
        log.error("Unexpected error at {}: {}", request.getRequestURI(), ex.getMessage(), ex);

        var response = new ErrorResponse(
                500,
                "INTERNAL",
                "Internal server error",
                request.getRequestURI()
        );

        return ResponseEntity.internalServerError().body(response);
    }
}
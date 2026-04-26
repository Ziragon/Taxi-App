package com.example.shared.exception.base;

import com.example.shared.exception.common.ResourceNotFoundException;
import com.example.shared.exception.common.ValidationException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.LinkedHashMap;
import java.util.Map;

public abstract class AbstractExceptionHandler {

    protected final Logger log = LoggerFactory.getLogger(getClass());

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

    @ExceptionHandler(BaseException.class)
    public ResponseEntity<ErrorResponse> onBaseException(BaseException ex, HttpServletRequest request) {
        return handleBaseException(ex, request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> onValidation(MethodArgumentNotValidException ex,
                                                      HttpServletRequest request) {
        Map<String, String> details = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                details.put(error.getField(), error.getDefaultMessage())
        );
        log.warn("Validation failed at {}: {}", request.getRequestURI(), details);

        var response = new ErrorResponse(422, "VALIDATION", "Validation failed",
                request.getRequestURI(), details);
        return ResponseEntity.unprocessableContent().body(response);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> onUnreadable(HttpMessageNotReadableException ex,
                                                      HttpServletRequest request) {
        return handleBaseException(
                new ValidationException("Malformed request body"), request);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> onMethodNotSupported(HttpRequestMethodNotSupportedException ex,
                                                              HttpServletRequest request) {
        return handleBaseException(
                new ValidationException("Method '%s' is not supported".formatted(ex.getMethod())), request);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> onMediaType(HttpMediaTypeNotSupportedException ex,
                                                     HttpServletRequest request) {
        return handleBaseException(
                new ValidationException("Content type '%s' is not supported".formatted(ex.getContentType())), request);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> onMissingParam(MissingServletRequestParameterException ex,
                                                        HttpServletRequest request) {
        return handleBaseException(
                new ValidationException("Required parameter '%s' is missing".formatted(ex.getParameterName())), request);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> onTypeMismatch(MethodArgumentTypeMismatchException ex,
                                                        HttpServletRequest request) {
        String type = ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown";
        return handleBaseException(
                new ValidationException("Parameter '%s' must be of type '%s'".formatted(ex.getName(), type)), request);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> onNoResource(NoResourceFoundException ex,
                                                      HttpServletRequest request) {
        return handleBaseException(
                new ResourceNotFoundException("Resource not found"), request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> onGeneric(Exception ex, HttpServletRequest request) {
        log.error("Unexpected error at {}: {}", request.getRequestURI(), ex.getMessage(), ex);

        var response = new ErrorResponse(500, "INTERNAL", "Internal server error",
                request.getRequestURI());
        return ResponseEntity.internalServerError().body(response);
    }
}
package com.example.userservice.exception.handler;

import com.example.shared.exception.base.BaseException;
import com.example.shared.exception.base.ErrorResponse;
import com.example.shared.exception.base.AbstractExceptionHandler;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler extends AbstractExceptionHandler {

    @ExceptionHandler(BaseException.class)
    public ResponseEntity<ErrorResponse> onBaseException(BaseException ex, HttpServletRequest request) {
        return handleBaseException(ex, request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> onValidation(MethodArgumentNotValidException ex,
                                                      HttpServletRequest request) {
        return handleValidation(ex, request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> onUnreadable(HttpMessageNotReadableException ex,
                                                      HttpServletRequest request) {
        return handleUnreadable(ex, request);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> onMethodNotSupported(HttpRequestMethodNotSupportedException ex,
                                                              HttpServletRequest request) {
        return handleMethodNotSupported(ex, request);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> onMediaType(HttpMediaTypeNotSupportedException ex,
                                                     HttpServletRequest request) {
        return handleMediaType(ex, request);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> onMissingParam(MissingServletRequestParameterException ex,
                                                        HttpServletRequest request) {
        return handleMissingParam(ex, request);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> onTypeMismatch(MethodArgumentTypeMismatchException ex,
                                                        HttpServletRequest request) {
        return handleTypeMismatch(ex, request);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> onNoResource(NoResourceFoundException ex,
                                                      HttpServletRequest request) {
        return handleNoResource(ex, request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> onGeneric(Exception ex, HttpServletRequest request) {
        return handleGeneric(ex, request);
    }

    // ── Сюда можно добавить обработчики специфичные только для user-service ──

    // @ExceptionHandler(SomeUserServiceSpecificException.class)
    // public ResponseEntity<ErrorResponse> onSpecific(...) { ... }
}
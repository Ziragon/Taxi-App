package com.example.paymentservice.exception.handler;

import com.example.paymentservice.exception.*;
import com.example.shared.exception.base.AbstractExceptionHandler;
import com.example.shared.exception.base.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler extends AbstractExceptionHandler {

    @ExceptionHandler({
            PaymentMethodNotFoundException.class,
            TransactionNotFoundException.class,
            DriverPayoutAccountNotFoundException.class
    })
    public ResponseEntity<ErrorResponse> handleNotFound(
            com.example.shared.exception.base.BaseException ex,
            HttpServletRequest request) {
        return handleBaseException(ex, request);
    }

    @ExceptionHandler({
            DuplicatePaymentMethodException.class
    })
    public ResponseEntity<ErrorResponse> handleConflict(
            com.example.shared.exception.base.BaseException ex,
            HttpServletRequest request) {
        return handleBaseException(ex, request);
    }

    @ExceptionHandler({
            InvalidPaymentOperationException.class,
            PaymentMethodNotActiveException.class,
            DefaultPaymentMethodException.class,
            PayoutAccountNotVerifiedException.class
    })
    public ResponseEntity<ErrorResponse> handleBadRequest(
            com.example.shared.exception.base.BaseException ex,
            HttpServletRequest request) {
        return handleBaseException(ex, request);
    }

    @ExceptionHandler({
            PaymentProcessingException.class,
            StripeException.class
    })
    public ResponseEntity<ErrorResponse> handleInternal(
            com.example.shared.exception.base.BaseException ex,
            HttpServletRequest request) {
        return handleBaseException(ex, request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {
        return handleValidation(ex, request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadableException(
            HttpMessageNotReadableException ex,
            HttpServletRequest request) {
        return handleUnreadable(ex, request);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupportedException(
            HttpRequestMethodNotSupportedException ex,
            HttpServletRequest request) {
        return handleMethodNotSupported(ex, request);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMediaTypeException(
            HttpMediaTypeNotSupportedException ex,
            HttpServletRequest request) {
        return handleMediaType(ex, request);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParamException(
            MissingServletRequestParameterException ex,
            HttpServletRequest request) {
        return handleMissingParam(ex, request);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatchException(
            MethodArgumentTypeMismatchException ex,
            HttpServletRequest request) {
        return handleTypeMismatch(ex, request);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResourceException(
            NoResourceFoundException ex,
            HttpServletRequest request) {
        return handleNoResource(ex, request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex,
            HttpServletRequest request) {
        return handleGeneric(ex, request);
    }
}

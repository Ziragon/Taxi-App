package com.example.tripservice.exception.handler;

import com.example.shared.exception.base.AbstractExceptionHandler;
import com.example.shared.exception.base.ErrorResponse;
import com.example.shared.exception.common.ResourceAlreadyExistsException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestControllerAdvice
public class GlobalExceptionHandler extends AbstractExceptionHandler {

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> onDataIntegrity(DataIntegrityViolationException ex,
                                                         HttpServletRequest request) {
        return handleBaseException(extractIntegrityException(ex), request);
    }

    private ResourceAlreadyExistsException extractIntegrityException(DataIntegrityViolationException ex) {
        String raw = ex.getMessage() != null ? ex.getMessage() : "";
        Matcher matcher = Pattern.compile("Key \\((.+?)\\)=\\((.+?)\\) already exists").matcher(raw);

        return matcher.find()
                ? new ResourceAlreadyExistsException("Resource", matcher.group(1), matcher.group(2))
                : new ResourceAlreadyExistsException("Duplicate value violates unique constraint");
    }
}
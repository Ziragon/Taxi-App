package com.example.notificationservice.exception;

import com.example.shared.exception.base.AbstractExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler extends AbstractExceptionHandler {
    // Здесь добавляем только специфичные для notification-service если понадобятся
}

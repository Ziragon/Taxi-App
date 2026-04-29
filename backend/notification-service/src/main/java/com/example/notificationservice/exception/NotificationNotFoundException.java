package com.example.notificationservice.exception;

import com.example.shared.exception.base.BaseException;
import com.example.shared.exception.base.ErrorType;

public class NotificationNotFoundException extends BaseException {

    public NotificationNotFoundException(Long id) {
        super("Notification not found with id: %d".formatted(id), ErrorType.NOT_FOUND);
    }
}

package com.example.notificationservice.exception;

import com.example.shared.exception.base.BaseException;
import com.example.shared.exception.base.ErrorType;

public class NotificationDeliveryException extends BaseException {

    public NotificationDeliveryException(Long recipientId, String reason) {
        super("Failed to deliver notification to recipientId=%d: %s"
                .formatted(recipientId, reason), ErrorType.INTERNAL);
    }
}

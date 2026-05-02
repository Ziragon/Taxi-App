package com.example.notificationservice.consumer;

import com.example.notificationservice.dto.NotificationEventDto;
import com.example.notificationservice.entity.Notification;
import com.example.notificationservice.entity.enums.Channel;
import com.example.notificationservice.entity.enums.EventType;
import com.example.notificationservice.entity.enums.RecipientType;
import com.example.notificationservice.service.NotificationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.util.Map;

import static com.example.notificationservice.config.RabbitMqConfig.NOTIFICATION_QUEUE;

@Slf4j
@Component
@RequiredArgsConstructor
public class UniversalNotificationConsumer {

    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;
    private final MessageConverter messageConverter;

    @RabbitListener(queues = NOTIFICATION_QUEUE, containerFactory = "rabbitListenerContainerFactory")
    public void consume(
            Message message,
            @Header(AmqpHeaders.RECEIVED_ROUTING_KEY) String routingKey
    ) {
        Object rawEvent = messageConverter.fromMessage(message);

        log.debug("Received event: routingKey={}, type={}, messageId={}",
                routingKey,
                rawEvent != null ? rawEvent.getClass().getSimpleName() : "null",
                message.getMessageProperties().getMessageId()
        );

        try {
            NotificationEventDto dto = parseEvent(rawEvent, routingKey);

            if (dto == null) {
                log.warn("Skipping unknown event: routingKey={}", routingKey);
                return;
            }

            Notification notification = notificationService.save(dto);
            notificationService.sendToUser(notification);
            notificationService.markSent(notification.getId());

            log.info("Notification delivered: id={}, recipientId={}, event={}, routingKey={}",
                    notification.getId(),
                    notification.getRecipientId(),
                    notification.getEventType(),
                    routingKey
            );

        } catch (Exception e) {
            log.error("Failed to process event: routingKey={}, error={}",
                    routingKey, e.getMessage(), e);

            try {
                NotificationEventDto dto = parseEvent(rawEvent, routingKey);
                if (dto != null) {
                    Notification notification = notificationService.save(dto);
                    notificationService.markFailed(notification.getId());
                }
            } catch (Exception saveEx) {
                log.error("Failed to save failed notification", saveEx);
            }

            throw new RuntimeException("Failed to process notification event", e);
        }
    }

    @SuppressWarnings("unchecked")
    private NotificationEventDto parseEvent(Object rawEvent, String routingKey) {

        if ("user.registered".equals(routingKey)) {
            Map<String, Object> event = (Map<String, Object>) rawEvent;

            Long accountId = event.get("accountId") instanceof Number number
                    ? number.longValue()
                    : Long.parseLong(event.get("accountId").toString());

            return NotificationEventDto.builder()
                    .tripId(null)
                    .eventType(EventType.USER_REGISTERED)
                    .recipientType(RecipientType.PASSENGER)
                    .recipientId(accountId)
                    .channel(Channel.PUSH)
                    .message("Welcome to TaxiApp! Your account has been created.")
                    .build();
        }

        if (routingKey.startsWith("notification.")) {
            if (rawEvent instanceof NotificationEventDto dto) {
                return dto;
            }

            if (rawEvent instanceof Map) {
                return objectMapper.convertValue(rawEvent, NotificationEventDto.class);
            }
        }

        return null;
    }
}
package com.example.notificationservice.consumer;

import com.example.notificationservice.config.RabbitMqConfig;
import com.example.notificationservice.dto.NotificationEventDto;
import com.example.notificationservice.entity.Notification;
import com.example.notificationservice.service.NotificationService;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventConsumer {

    private final NotificationService notificationService;

    @RabbitListener(
            queues = RabbitMqConfig.NOTIFICATION_QUEUE,
            ackMode = "MANUAL"
    )
    public void consume(
            NotificationEventDto event,
            Channel channel,
            @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag
    ) throws IOException {
        log.debug("Received event: type={}, recipientId={}",
                event.eventType(), event.recipientId());

        Notification notification = notificationService.save(event);

        try {

            notificationService.sendToUser(notification);

            notificationService.markSent(notification.getId());

            channel.basicAck(deliveryTag, false);

            log.info("Notification delivered: id={}, recipientId={}, event={}",
                    notification.getId(),
                    notification.getRecipientId(),
                    notification.getEventType()
            );
        } catch (Exception e) {
            log.error("Failed to deliver notification: id={}, recipientId={}, error={}",
                    notification.getId(),
                    notification.getRecipientId(),
                    e.getMessage(),
                    e
            );

            notificationService.markFailed(notification.getId());

            channel.basicNack(deliveryTag, false, false);
        }
    }
}

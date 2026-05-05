package com.example.notificationservice.consumer;

import com.example.notificationservice.client.UserServiceClient;
import com.example.notificationservice.dto.DriverProfileSnapshot;
import com.example.notificationservice.dto.NotificationEventDto;
import com.example.notificationservice.dto.PassengerProfileSnapshot;
import com.example.notificationservice.entity.Notification;
import com.example.notificationservice.entity.enums.Channel;
import com.example.notificationservice.entity.enums.EventType;
import com.example.notificationservice.entity.enums.RecipientType;
import com.example.notificationservice.service.NotificationService;
import com.example.shared.dto.event.TripOfferEvent;
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
    private final UserServiceClient userServiceClient;

    @RabbitListener(queues = NOTIFICATION_QUEUE, containerFactory = "rabbitListenerContainerFactory")
    public void consume(
            Message message,
            @Header(AmqpHeaders.RECEIVED_ROUTING_KEY) String routingKey
    ) {
        Object rawEvent = messageConverter.fromMessage(message);

        try {
            NotificationEventDto dto = parseEvent(rawEvent, routingKey);

            if (dto == null) {
                log.warn("Skipping unknown event: routingKey={}", routingKey);
                return;
            }

            Notification notification = notificationService.save(dto);

            DriverProfileSnapshot driverProfile = null;
            PassengerProfileSnapshot passengerProfile = null;

            try {
                if (routingKey.equals("notification.trip.driver_assigned")) {
                    Map<String, Object> event = (Map<String, Object>) rawEvent;
                    Long driverId = getLong(event, "driverId");
                    driverProfile = userServiceClient.getDriverProfile(driverId);
                }

                if (routingKey.equals("notification.trip.offer")) {
                    TripOfferEvent event = rawEvent instanceof TripOfferEvent e
                            ? e
                            : objectMapper.convertValue(rawEvent, TripOfferEvent.class);
                    passengerProfile = userServiceClient.getPassengerProfile(event.passengerId());
                }
            } catch (Exception e) {
                log.warn("Failed to enrich notification id={} with profile: {}",
                        notification.getId(), e.getMessage());
            }

            notificationService.sendToUser(notification, driverProfile, passengerProfile);
            notificationService.markSent(notification.getId());

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

        // Специфичные события — ПЕРВЫМИ
        switch (routingKey) {

            case "user.registered" -> {
                Map<String, Object> event = (Map<String, Object>) rawEvent;
                Long accountId = getLong(event, "accountId");
                return NotificationEventDto.builder()
                        .tripId(null)
                        .eventType(EventType.USER_REGISTERED)
                        .recipientType(RecipientType.PASSENGER)
                        .recipientId(accountId)
                        .channel(Channel.PUSH)
                        .message("Добро пожаловать в TaxiApp!")
                        .build();
            }

            case "notification.trip.offer" -> {
                TripOfferEvent event = rawEvent instanceof TripOfferEvent e
                        ? e
                        : objectMapper.convertValue(rawEvent, TripOfferEvent.class);

                String message = String.format(
                        "Новый заказ: %s → %s, %.1f км, %.0f мин, %.2f ₽",
                        event.originAddress(),
                        event.destinationAddress(),
                        event.distanceKm(),
                        event.durationMin(),
                        event.price()
                );
                return NotificationEventDto.builder()
                        .tripId(event.tripId())
                        .eventType(EventType.TRIP_OFFER)
                        .recipientType(RecipientType.DRIVER)
                        .recipientId(event.driverId())
                        .channel(Channel.PUSH)
                        .message(message)
                        .build();
            }

            case "notification.trip.offer.expired" -> {
                Map<String, Object> event = (Map<String, Object>) rawEvent;
                return NotificationEventDto.builder()
                        .tripId(getLong(event, "tripId"))
                        .eventType(EventType.TRIP_OFFER_EXPIRED)
                        .recipientType(RecipientType.DRIVER)
                        .recipientId(getLong(event, "driverId"))
                        .channel(Channel.PUSH)
                        .message("Время ответа на заказ истекло")
                        .build();
            }

            case "notification.trip.driver_assigned" ->
            { return buildNotificationFromMap((Map<String, Object>) rawEvent, EventType.DRIVER_ASSIGNED); }

            case "notification.trip.started" ->
            { return buildNotificationFromMap((Map<String, Object>) rawEvent, EventType.TRIP_STARTED); }

            case "notification.trip.completed" ->
            { return buildNotificationFromMap((Map<String, Object>) rawEvent, EventType.TRIP_COMPLETED); }

            case "notification.trip.cancelled" ->
            { return buildNotificationFromMap((Map<String, Object>) rawEvent, EventType.TRIP_CANCELLED); }

            case "notification.payment.succeeded", "notification.payout.succeeded" ->
            { return buildNotificationFromMap((Map<String, Object>) rawEvent, EventType.PAYMENT_SUCCEEDED); }

            case "notification.payment.failed" ->
            { return buildNotificationFromMap((Map<String, Object>) rawEvent, EventType.PAYMENT_FAILED); }

            case "notification.refund.succeeded" ->
            { return buildNotificationFromMap((Map<String, Object>) rawEvent, EventType.REFUND_SUCCEEDED); }
        }

        if (routingKey.startsWith("notification.")) {
            if (rawEvent instanceof NotificationEventDto dto) {
                return dto;
            }
            if (rawEvent instanceof Map) {
                try {
                    return objectMapper.convertValue(rawEvent, NotificationEventDto.class);
                } catch (Exception e) {
                    log.warn("Failed to convert Map to NotificationEventDto for routingKey={}", routingKey);
                }
            }
        }

        return null;
    }

    private NotificationEventDto buildNotificationFromMap(Map<String, Object> event, EventType eventType) {
        return NotificationEventDto.builder()
                .tripId(getLong(event, "tripId"))
                .eventType(eventType)
                .recipientType(RecipientType.valueOf((String) event.get("recipientType")))
                .recipientId(getLong(event, "recipientId"))
                .channel(Channel.PUSH)
                .message((String) event.get("message"))
                .build();
    }

    private Long getLong(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value instanceof Number n ? n.longValue() : Long.parseLong(value.toString());
    }
}
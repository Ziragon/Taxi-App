package com.example.tripservice.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;

import static com.example.tripservice.config.RabbitMQConfig.NOTIFICATION_EXCHANGE;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationPublisher {

    private final RabbitTemplate rabbitTemplate;
    private static final BigDecimal DRIVER_SHARE = new BigDecimal("0.80");

    public void publishDriverAssigned(Long passengerId, Long tripId, Long driverId) {
        Map<String, Object> payload = Map.of(
                "recipientId", passengerId,
                "recipientType", "PASSENGER",
                "tripId", tripId,
                "eventType", "DRIVER_ASSIGNED",
                "channel", "PUSH",
                "message", "Водитель найден и едет к вам!"
        );
        rabbitTemplate.convertAndSend(NOTIFICATION_EXCHANGE, "notification.trip.driver_assigned", payload);
        log.info("Published DRIVER_ASSIGNED: tripId={}", tripId);
    }

    public void publishTripStarted(Long passengerId, Long tripId) {
        Map<String, Object> payload = Map.of(
                "recipientId", passengerId,
                "recipientType", "PASSENGER",
                "tripId", tripId,
                "eventType", "TRIP_STARTED",
                "channel", "PUSH",
                "message", "Поездка началась. Приятного пути!"
        );
        rabbitTemplate.convertAndSend(NOTIFICATION_EXCHANGE, "notification.trip.started", payload);
        log.info("Published TRIP_STARTED: tripId={}", tripId);
    }

    public void publishTripCompleted(Long passengerId, Long tripId, BigDecimal amount) {
        Map<String, Object> payload = Map.of(
                "recipientId", passengerId,
                "recipientType", "PASSENGER",
                "tripId", tripId,
                "eventType", "TRIP_COMPLETED",
                "channel", "PUSH",
                "message", "Поездка завершена. Списано %.2f ₽".formatted(amount)
        );
        rabbitTemplate.convertAndSend(NOTIFICATION_EXCHANGE, "notification.trip.completed", payload);
        log.info("Published TRIP_COMPLETED: tripId={}", tripId);
    }

    public void publishTripCancelled(Long recipientId, Long tripId, String reason) {
        Map<String, Object> payload = Map.of(
                "recipientId", recipientId,
                "recipientType", "PASSENGER",
                "tripId", tripId,
                "eventType", "TRIP_CANCELLED",
                "channel", "PUSH",
                "message", "Поездка отменена: " + reason
        );
        rabbitTemplate.convertAndSend(NOTIFICATION_EXCHANGE, "notification.trip.cancelled", payload);
        log.info("Published TRIP_CANCELLED: tripId={}", tripId);
    }

    public void publishPaymentSucceeded(Long passengerId, Long driverId, Long tripId, BigDecimal amount) {
        Map<String, Object> passengerPayload = Map.of(
                "recipientId", passengerId,
                "recipientType", "PASSENGER",
                "tripId", tripId,
                "eventType", "PAYMENT_SUCCEEDED",
                "channel", "PUSH",
                "message", "Оплата прошла: %.2f ₽".formatted(amount)
        );
        rabbitTemplate.convertAndSend(NOTIFICATION_EXCHANGE, "notification.payment.succeeded", passengerPayload);

        BigDecimal driverAmount = amount.multiply(DRIVER_SHARE).setScale(2, java.math.RoundingMode.HALF_UP);
        Map<String, Object> driverPayload = Map.of(
                "recipientId", driverId,
                "recipientType", "DRIVER",
                "tripId", tripId,
                "eventType", "PAYMENT_SUCCEEDED",
                "channel", "PUSH",
                "message", "Выплата: %.2f ₽".formatted(driverAmount)
        );
        rabbitTemplate.convertAndSend(NOTIFICATION_EXCHANGE, "notification.payout.succeeded", driverPayload);
    }

    public void publishPaymentFailed(Long passengerId, Long tripId, String reason) {
        Map<String, Object> payload = Map.of(
                "recipientId", passengerId,
                "recipientType", "PASSENGER",
                "tripId", tripId,
                "eventType", "PAYMENT_FAILED",
                "channel", "PUSH",
                "message", "Ошибка оплаты. Проверьте карту."
        );
        rabbitTemplate.convertAndSend(NOTIFICATION_EXCHANGE, "notification.payment.failed", payload);
    }

    public void publishRefundSucceeded(Long passengerId, Long tripId, BigDecimal amount) {
        Map<String, Object> payload = Map.of(
                "recipientId", passengerId,
                "recipientType", "PASSENGER",
                "tripId", tripId,
                "eventType", "REFUND_SUCCEEDED",
                "channel", "PUSH",
                "message", "Возврат: %.2f ₽".formatted(amount)
        );
        rabbitTemplate.convertAndSend(NOTIFICATION_EXCHANGE, "notification.refund.succeeded", payload);
    }
}
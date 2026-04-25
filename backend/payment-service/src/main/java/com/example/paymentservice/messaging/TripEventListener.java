package com.example.paymentservice.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import static com.example.paymentservice.config.RabbitMQConfig.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class TripEventListener {

    // Здесь будут методы для обработки событий от Trip Service

    @RabbitListener(queues = TRIP_COMPLETED_QUEUE)
    public void handleTripCompleted(Object event) {
        log.info("Received trip completed event: {}", event);
        // TODO: реализовать логику обработки платежей
    }

    @RabbitListener(queues = REFUND_REQUESTED_QUEUE)
    public void handleRefundRequested(Object event) {
        log.info("Received refund requested event: {}", event);
        // TODO: реализовать логику возврата
    }
}

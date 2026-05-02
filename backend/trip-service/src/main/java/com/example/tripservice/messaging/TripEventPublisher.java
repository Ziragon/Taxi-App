package com.example.tripservice.messaging;

import com.example.shared.dto.event.TripCompletedEvent;
import com.example.shared.dto.event.RefundRequestedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import static com.example.tripservice.config.RabbitMQConfig.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class TripEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publishTripCompleted(TripCompletedEvent event) {
        log.info("Publishing TripCompletedEvent: tripId={}, amount={}",
                event.tripId(), event.amount());
        rabbitTemplate.convertAndSend(
                TRIP_EXCHANGE,
                TRIP_COMPLETED_ROUTING_KEY,
                event
        );
    }

    public void publishRefundRequested(RefundRequestedEvent event) {
        log.info("Publishing RefundRequestedEvent: tripId={}, amount={}",
                event.tripId(), event.amount());
        rabbitTemplate.convertAndSend(
                TRIP_EXCHANGE,
                REFUND_REQUESTED_ROUTING_KEY,
                event
        );
    }
}

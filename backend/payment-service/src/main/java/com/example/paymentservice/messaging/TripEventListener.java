package com.example.paymentservice.messaging;

import com.example.shared.dto.event.RefundRequestedEvent;
import com.example.shared.dto.event.TripCompletedEvent;
import com.example.paymentservice.service.TransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import static com.example.paymentservice.config.RabbitMQConfig.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class TripEventListener {

    private final TransactionService transactionService;

    @RabbitListener(queues = TRIP_COMPLETED_QUEUE)
    public void onTripCompleted(TripCompletedEvent event) {
        log.info("Received TripCompletedEvent: tripId={}, amount={}",
                event.tripId(), event.amount());

        try {
            transactionService.createCharge(
                    event.tripId(),
                    event.passengerId(),
                    event.driverId(),
                    null,
                    event.amount(),
                    event.currency()
            );
        } catch (Exception e) {
            log.error("Failed to create charge for trip {}: {}", event.tripId(), e.getMessage());
        }
    }

    @RabbitListener(queues = REFUND_REQUESTED_QUEUE)
    public void onRefundRequested(RefundRequestedEvent event) {
        log.info("Received RefundRequestedEvent: tripId={}, amount={}",
                event.tripId(), event.amount());

        try {

            var transaction = transactionService.getByTripId(event.tripId());

            transactionService.createRefund(
                    transaction.getId(),
                    event.amount(),
                    event.reason()
            );
        } catch (Exception e) {
            log.error("Failed to create refund for trip {}: {}", event.tripId(), e.getMessage());
        }
    }
}

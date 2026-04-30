package com.example.paymentservice.messaging;

import com.example.paymentservice.service.TransactionService;
import com.example.shared.dto.event.RefundRequestedEvent;
import com.example.shared.dto.event.TripCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import static com.example.paymentservice.config.RabbitMQConfig.REFUND_REQUESTED_QUEUE;
import static com.example.paymentservice.config.RabbitMQConfig.TRIP_COMPLETED_QUEUE;

@Slf4j
@Component
@RequiredArgsConstructor
public class TripEventListener {

    private final TransactionService transactionService;

    @RabbitListener(queues = TRIP_COMPLETED_QUEUE)
    public void handleTripCompleted(TripCompletedEvent event) {
        log.info("Received TripCompletedEvent: tripId={}, passengerId={}, amount={}",
                event.tripId(), event.passengerId(), event.amount());

        try {
            transactionService.createCharge(
                    event.tripId(),
                    event.passengerId(),
                    event.driverId(),
                    null,
                    event.amount(),
                    event.currency()
            );

            log.info("Charge created successfully for trip {}", event.tripId());

        } catch (Exception e) {
            log.error("Failed to process charge for trip {}: {}", event.tripId(), e.getMessage(), e);
            throw e;
        }
    }

    @RabbitListener(queues = REFUND_REQUESTED_QUEUE)
    public void handleRefundRequested(RefundRequestedEvent event) {
        log.info("Received RefundRequestedEvent: tripId={}, passengerId={}, amount={}",
                event.tripId(), event.passengerId(), event.amount());

        try {

            var originalTransaction = transactionService.getByTripId(event.tripId());

            transactionService.createRefund(
                    originalTransaction.getId(),
                    event.amount(),
                    event.reason()
            );

            log.info("Refund created successfully for trip {}", event.tripId());

        } catch (Exception e) {
            log.error("Failed to process refund for trip {}: {}", event.tripId(), e.getMessage(), e);
            throw e;
        }
    }
}

package com.example.paymentservice.messaging;

import com.example.shared.dto.event.RefundRequestedEvent;
import com.example.shared.dto.event.TripCompletedEvent;
import com.example.paymentservice.entity.Transaction;
import com.example.paymentservice.entity.enums.TransactionStatus;
import com.example.paymentservice.service.TransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static com.example.paymentservice.config.RabbitMQConfig.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class TripEventListener {

    private final TransactionService transactionService;
    private static final BigDecimal DRIVER_SHARE = new BigDecimal("0.80");

    @RabbitListener(queues = TRIP_COMPLETED_QUEUE)
    public void onTripCompleted(TripCompletedEvent event) {
        log.info("Received TripCompletedEvent: tripId={}, amount={}", event.tripId(), event.amount());

        Transaction charge;
        try {
            charge = transactionService.captureHold(event.tripId());
            log.info("Hold captured for trip {}: transactionId={}", event.tripId(), charge.getId());
        } catch (Exception e) {
            log.error("Failed to capture hold for trip {}, fallback to charge: {}",
                    event.tripId(), e.getMessage());

            try {
                charge = transactionService.createCharge(
                        event.tripId(),
                        event.passengerId(),
                        event.driverId(),
                        null,
                        event.amount(),
                        event.currency()
                );
            } catch (Exception ex) {
                log.error("Fallback charge also failed for trip {}: {}", event.tripId(), ex.getMessage());
                return;
            }
        }

        if (charge.getStatus() != TransactionStatus.SUCCEEDED) {
            log.warn("Charge/capture not succeeded for trip {}, status={}",
                    event.tripId(), charge.getStatus());
            return;
        }

        BigDecimal driverAmount = event.amount()
                .multiply(DRIVER_SHARE)
                .setScale(2, RoundingMode.HALF_UP);

        try {
            transactionService.createPayout(
                    event.tripId(),
                    event.passengerId(),
                    event.driverId(),
                    driverAmount,
                    event.currency()
            );
        } catch (Exception e) {
            log.error("CRITICAL: Capture succeeded but payout failed for trip {}: {}",
                    event.tripId(), e.getMessage());
        }
    }

    @RabbitListener(queues = REFUND_REQUESTED_QUEUE)
    public void onRefundRequested(RefundRequestedEvent event) {
        log.info("Received RefundRequestedEvent: tripId={}", event.tripId());

        try {
            Transaction transaction = transactionService.getByTripId(event.tripId());
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
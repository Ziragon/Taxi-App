package com.example.tripservice.messaging;

import com.example.shared.dto.event.PaymentFailedEvent;
import com.example.shared.dto.event.PaymentSucceededEvent;
import com.example.shared.dto.event.RefundSucceededEvent;
import com.example.tripservice.service.trip.TripPaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import static com.example.tripservice.config.RabbitMQConfig.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventListener {

    private final TripPaymentService tripPaymentService;

    @RabbitListener(queues = PAYMENT_SUCCEEDED_QUEUE)
    public void onPaymentSucceeded(PaymentSucceededEvent event) {
        log.info("Received PaymentSucceededEvent: tripId={}, transactionId={}",
                event.tripId(), event.transactionId());
        tripPaymentService.handlePaymentSucceeded(event);
    }

    @RabbitListener(queues = PAYMENT_FAILED_QUEUE)
    public void onPaymentFailed(PaymentFailedEvent event) {
        log.info("Received PaymentFailedEvent: tripId={}, reason={}",
                event.tripId(), event.reason());
        tripPaymentService.handlePaymentFailed(event);
    }

    @RabbitListener(queues = REFUND_SUCCEEDED_QUEUE)
    public void onRefundSucceeded(RefundSucceededEvent event) {
        log.info("Received RefundSucceededEvent: tripId={}, amount={}",
                event.tripId(), event.amount());
        tripPaymentService.handleRefundSucceeded(event);
    }
}

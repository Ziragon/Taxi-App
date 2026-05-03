package com.example.paymentservice.messaging;

import com.example.shared.dto.event.PaymentFailedEvent;
import com.example.shared.dto.event.PaymentSucceededEvent;
import com.example.shared.dto.event.PayoutSucceededEvent;
import com.example.shared.dto.event.RefundSucceededEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import static com.example.paymentservice.config.RabbitMQConfig.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publishPaymentSucceeded(PaymentSucceededEvent event) {
        log.info("Publishing PaymentSucceededEvent: tripId={}, amount={}",
                event.tripId(), event.amount());
        rabbitTemplate.convertAndSend(
                PAYMENT_EXCHANGE,
                PAYMENT_SUCCEEDED_ROUTING_KEY,
                event
        );
    }

    public void publishPaymentFailed(PaymentFailedEvent event) {
        log.info("Publishing PaymentFailedEvent: tripId={}, reason={}",
                event.tripId(), event.reason());
        rabbitTemplate.convertAndSend(
                PAYMENT_EXCHANGE,
                PAYMENT_FAILED_ROUTING_KEY,
                event
        );
    }

    public void publishRefundSucceeded(RefundSucceededEvent event) {
        log.info("Publishing RefundSucceededEvent: tripId={}, amount={}",
                event.tripId(), event.amount());
        rabbitTemplate.convertAndSend(
                PAYMENT_EXCHANGE,
                REFUND_SUCCEEDED_ROUTING_KEY,
                event
        );
    }

    public void publishPayoutSucceeded(PayoutSucceededEvent event) {
        log.info("Publishing PayoutSucceededEvent: tripId={}, driverId={}, amount={}",
                event.tripId(), event.driverId(), event.amount());
        rabbitTemplate.convertAndSend(PAYMENT_EXCHANGE, PAYOUT_SUCCEEDED_ROUTING_KEY, event);
    }

}
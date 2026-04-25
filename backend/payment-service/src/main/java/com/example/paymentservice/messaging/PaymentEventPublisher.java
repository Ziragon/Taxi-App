package com.example.paymentservice.messaging;

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

    public void publishPaymentSucceeded(Object event) {
        log.info("Publishing payment succeeded event: {}", event);
        rabbitTemplate.convertAndSend(
                PAYMENT_EXCHANGE,
                PAYMENT_SUCCEEDED_ROUTING_KEY,
                event
        );
    }

    public void publishPaymentFailed(Object event) {
        log.info("Publishing payment failed event: {}", event);
        rabbitTemplate.convertAndSend(
                PAYMENT_EXCHANGE,
                PAYMENT_FAILED_ROUTING_KEY,
                event
        );
    }
}

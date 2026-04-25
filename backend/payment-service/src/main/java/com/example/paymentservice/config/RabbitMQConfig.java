package com.example.paymentservice.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // Exchanges
    public static final String PAYMENT_EXCHANGE = "payment.exchange";
    public static final String TRIP_EXCHANGE = "trip.exchange";

    // Queues
    public static final String TRIP_COMPLETED_QUEUE = "payment.trip.completed";
    public static final String PAYMENT_SUCCEEDED_QUEUE = "payment.succeeded";
    public static final String PAYMENT_FAILED_QUEUE = "payment.failed";
    public static final String REFUND_REQUESTED_QUEUE = "payment.refund.requested";

    // Routing Keys
    public static final String TRIP_COMPLETED_ROUTING_KEY = "trip.completed";
    public static final String PAYMENT_SUCCEEDED_ROUTING_KEY = "payment.succeeded";
    public static final String PAYMENT_FAILED_ROUTING_KEY = "payment.failed";
    public static final String REFUND_REQUESTED_ROUTING_KEY = "refund.requested";

    // Exchanges

    @Bean
    public TopicExchange paymentExchange() {
        return ExchangeBuilder
                .topicExchange(PAYMENT_EXCHANGE)
                .durable(true)
                .build();
    }

    @Bean
    public TopicExchange tripExchange() {
        return ExchangeBuilder
                .topicExchange(TRIP_EXCHANGE)
                .durable(true)
                .build();
    }

    // Queues

    @Bean
    public Queue tripCompletedQueue() {
        return QueueBuilder
                .durable(TRIP_COMPLETED_QUEUE)
                .build();
    }

    @Bean
    public Queue paymentSucceededQueue() {
        return QueueBuilder
                .durable(PAYMENT_SUCCEEDED_QUEUE)
                .build();
    }

    @Bean
    public Queue paymentFailedQueue() {
        return QueueBuilder
                .durable(PAYMENT_FAILED_QUEUE)
                .build();
    }

    @Bean
    public Queue refundRequestedQueue() {
        return QueueBuilder
                .durable(REFUND_REQUESTED_QUEUE)
                .build();
    }

    // Bindings

    @Bean
    public Binding tripCompletedBinding(Queue tripCompletedQueue, TopicExchange tripExchange) {
        return BindingBuilder
                .bind(tripCompletedQueue)
                .to(tripExchange)
                .with(TRIP_COMPLETED_ROUTING_KEY);
    }

    @Bean
    public Binding paymentSucceededBinding(Queue paymentSucceededQueue, TopicExchange paymentExchange) {
        return BindingBuilder
                .bind(paymentSucceededQueue)
                .to(paymentExchange)
                .with(PAYMENT_SUCCEEDED_ROUTING_KEY);
    }

    @Bean
    public Binding paymentFailedBinding(Queue paymentFailedQueue, TopicExchange paymentExchange) {
        return BindingBuilder
                .bind(paymentFailedQueue)
                .to(paymentExchange)
                .with(PAYMENT_FAILED_ROUTING_KEY);
    }

    @Bean
    public Binding refundRequestedBinding(Queue refundRequestedQueue, TopicExchange tripExchange) {
        return BindingBuilder
                .bind(refundRequestedQueue)
                .to(tripExchange)
                .with(REFUND_REQUESTED_ROUTING_KEY);
    }

    // Message Converter

    @Bean
    public MessageConverter messageConverter() {
        return new JacksonJsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter messageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter());
        return template;
    }
}

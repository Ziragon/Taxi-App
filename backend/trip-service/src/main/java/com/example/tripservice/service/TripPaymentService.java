package com.example.tripservice.service;

import com.example.shared.dto.event.PaymentFailedEvent;
import com.example.shared.dto.event.PaymentSucceededEvent;
import com.example.shared.dto.event.RefundSucceededEvent;
import com.example.tripservice.entity.Trip;
import com.example.tripservice.entity.enums.TripStatus;
import com.example.tripservice.exception.TripNotFoundException;
import com.example.tripservice.repository.TripRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TripPaymentService {

    private final TripRepository tripRepository;

    @Transactional
    public void handlePaymentSucceeded(PaymentSucceededEvent event) {
        Trip trip = tripRepository.findById(event.tripId())
                .orElseThrow(() -> new TripNotFoundException(event.tripId()));

        trip.setPaymentId(event.transactionId());
        trip.setStatus(TripStatus.COMPLETED);
        tripRepository.save(trip);

        log.info("Trip {} marked as COMPLETED after payment {}", event.tripId(), event.transactionId());

        // TODO: WebSocket уведомление пассажиру и водителю
    }

    @Transactional
    public void handlePaymentFailed(PaymentFailedEvent event) {
        Trip trip = tripRepository.findById(event.tripId())
                .orElseThrow(() -> new TripNotFoundException(event.tripId()));

        // Оставляем IN_PROGRESS — можно попробовать снова
        // или переводим в CANCELLED в зависимости от бизнес-логики
        log.warn("Payment failed for trip {}: {}", event.tripId(), event.reason());

        // TODO: WebSocket уведомление пассажиру — попробовать другую карту
        // TODO: логика retry или отмены
    }

    @Transactional
    public void handleRefundSucceeded(RefundSucceededEvent event) {
        Trip trip = tripRepository.findById(event.tripId())
                .orElseThrow(() -> new TripNotFoundException(event.tripId()));

        log.info("Refund succeeded for trip {}, amount={}", event.tripId(), event.amount());

        // TODO: WebSocket уведомление пассажиру
    }
}

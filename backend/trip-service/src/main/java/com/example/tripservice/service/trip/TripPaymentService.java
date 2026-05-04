package com.example.tripservice.service.trip;

import com.example.shared.dto.event.PaymentFailedEvent;
import com.example.shared.dto.event.PaymentSucceededEvent;
import com.example.shared.dto.event.RefundSucceededEvent;
import com.example.tripservice.entity.Trip;
import com.example.tripservice.entity.enums.TripStatus;
import com.example.tripservice.exception.TripNotFoundException;
import com.example.tripservice.messaging.NotificationPublisher;
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
    private final NotificationPublisher notificationPublisher;

    @Transactional
    public void handlePaymentSucceeded(PaymentSucceededEvent event) {
        Trip trip = tripRepository.findById(event.tripId())
                .orElseThrow(() -> new TripNotFoundException(event.tripId()));

        trip.setPaymentId(event.transactionId());
        trip.setStatus(TripStatus.COMPLETED);
        tripRepository.save(trip);

        log.info("Trip {} COMPLETED after payment {}", event.tripId(), event.transactionId());

        notificationPublisher.publishPaymentSucceeded(
                trip.getPassengerId(),
                trip.getDriverId(),
                trip.getId(),
                event.amount()
        );
    }

    @Transactional(readOnly = true)
    public void handlePaymentFailed(PaymentFailedEvent event) {
        Trip trip = tripRepository.findById(event.tripId())
                .orElseThrow(() -> new TripNotFoundException(event.tripId()));

        log.warn("Payment failed for trip {}: {}", event.tripId(), event.reason());

        notificationPublisher.publishPaymentFailed(
                trip.getPassengerId(),
                trip.getId(),
                event.reason()
        );
    }

    @Transactional(readOnly = true)
    public void handleRefundSucceeded(RefundSucceededEvent event) {
        Trip trip = tripRepository.findById(event.tripId())
                .orElseThrow(() -> new TripNotFoundException(event.tripId()));

        log.info("Refund succeeded for trip {}, amount={}", event.tripId(), event.amount());

        notificationPublisher.publishRefundSucceeded(
                trip.getPassengerId(),
                trip.getId(),
                event.amount()
        );
    }
}
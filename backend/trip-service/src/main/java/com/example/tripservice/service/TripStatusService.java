package com.example.tripservice.service;

import com.example.shared.dto.event.RefundRequestedEvent;
import com.example.shared.dto.event.TripCompletedEvent;
import com.example.tripservice.client.PaymentServiceClient;
import com.example.tripservice.dto.client.PaymentMethodResponse;
import com.example.tripservice.entity.Trip;
import com.example.tripservice.entity.enums.TripStatus;
import com.example.tripservice.exception.TripNotFoundException;
import com.example.tripservice.messaging.TripEventPublisher;
import com.example.tripservice.repository.TripRepository;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class TripStatusService {

    private final TripRepository tripRepository;
    private final TripEventPublisher tripEventPublisher;
    private final PaymentServiceClient paymentServiceClient;

    @Transactional
    public void assignDriver(Long tripId, Long driverId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new TripNotFoundException(tripId));

        try {
            PaymentMethodResponse paymentMethod = paymentServiceClient
                    .getDefaultPaymentMethod(trip.getPassengerId());
            log.info("Payment method verified for passenger {}: {} ****{}",
                    trip.getPassengerId(), paymentMethod.cardBrand(), paymentMethod.lastFour());
        } catch (FeignException.NotFound e) {
            log.warn("No payment method for passenger {}, cancelling trip {}",
                    trip.getPassengerId(), tripId);
            trip.setStatus(TripStatus.CANCELLED);
            tripRepository.save(trip);
            // TODO: уведомить пассажира
            return;
        }

        trip.setStatus(TripStatus.DRIVER_ASSIGNED);
        trip.setDriverId(driverId);
        tripRepository.save(trip);

        log.info("Driver {} assigned to trip {}", driverId, tripId);
        // TODO: WebSocket уведомление пассажиру
    }

    @Transactional
    public void startTrip(Long tripId, Long driverId) {
        Trip trip = getTripForDriver(tripId, driverId);

        trip.setStatus(TripStatus.IN_PROGRESS);
        tripRepository.save(trip);

        log.info("Trip {} started by driver {}", tripId, driverId);
        // TODO: WebSocket уведомление пассажиру
    }

    @Transactional
    public void completeTrip(Long tripId, Long driverId) {
        Trip trip = getTripForDriver(tripId, driverId);

        trip.setStatus(TripStatus.COMPLETED);
        tripRepository.save(trip);

        log.info("Trip {} completed by driver {}, publishing TripCompletedEvent", tripId, driverId);

        tripEventPublisher.publishTripCompleted(new TripCompletedEvent(
                trip.getId(),
                trip.getPassengerId(),
                trip.getDriverId(),
                trip.getPrice(),
                "usd",
                Instant.now()
        ));
    }

    @Transactional
    public void cancelTrip(Long tripId, Long passengerId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new TripNotFoundException(tripId));

        if (!trip.getPassengerId().equals(passengerId)) {
            throw new com.example.shared.exception.common.AccessDeniedException();
        }

        TripStatus currentStatus = trip.getStatus();
        trip.setStatus(TripStatus.CANCELLED);
        tripRepository.save(trip);

        log.info("Trip {} cancelled by passenger {}", tripId, passengerId);

        if (trip.getPaymentId() != null) {
            tripEventPublisher.publishRefundRequested(new RefundRequestedEvent(
                    trip.getId(),
                    trip.getPassengerId(),
                    trip.getDriverId(),
                    trip.getPrice(),
                    "Trip cancelled by passenger"
            ));
        }

        // TODO: WebSocket уведомление водителю если IN_PROGRESS
    }

    @Transactional
    public void cancelSearch(Long tripId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new TripNotFoundException(tripId));

        trip.setStatus(TripStatus.CANCELLED);
        tripRepository.save(trip);

        log.info("Search cancelled for trip {} — no drivers found", tripId);
        // TODO: WebSocket уведомление пассажиру
    }

    private Trip getTripForDriver(Long tripId, Long driverId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new TripNotFoundException(tripId));

        if (!driverId.equals(trip.getDriverId())) {
            throw new com.example.shared.exception.common.AccessDeniedException();
        }

        return trip;
    }
}

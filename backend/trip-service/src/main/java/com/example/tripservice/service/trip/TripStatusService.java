package com.example.tripservice.service.trip;

import com.example.shared.dto.enums.VehicleClass;
import com.example.shared.dto.event.RefundRequestedEvent;
import com.example.shared.dto.event.TripCompletedEvent;
import com.example.shared.exception.common.AccessDeniedException;
import com.example.tripservice.client.PaymentServiceClient;
import com.example.tripservice.dto.client.PaymentMethodResponse;
import com.example.tripservice.dto.data.PriceBreakdown;
import com.example.tripservice.dto.data.TariffDto;
import com.example.tripservice.dto.data.TripDto;
import com.example.tripservice.entity.Tariff;
import com.example.tripservice.entity.Trip;
import com.example.tripservice.entity.enums.TripStatus;
import com.example.tripservice.exception.TripNotFoundException;
import com.example.tripservice.messaging.NotificationPublisher;
import com.example.tripservice.messaging.TripEventPublisher;
import com.example.tripservice.repository.TripRepository;
import com.example.tripservice.service.pricing.TariffService;
import com.example.tripservice.util.StatusValidationUtil;
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
    private final TariffService tariffService;
    private final TripEventPublisher tripEventPublisher;
    private final PaymentServiceClient paymentServiceClient;
    private final ActiveTripCacheService activeTripCacheService;
    private final NotificationPublisher notificationPublisher;

    @Transactional
    public Trip setSearching(Long userId, Long tripId, VehicleClass vehicleClass) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new TripNotFoundException(tripId));

        StatusValidationUtil.assertTripNotActive(trip);

        if (!trip.getPassengerId().equals(userId)) {
            throw new AccessDeniedException("You are not owner of this trip");
        }

        Tariff tariff = tariffService.getByVehicleClass(vehicleClass);
        TariffDto tariffDto = tariffService.calculatePrice(tariff, TripDto.from(trip, null, null));

        trip.setTripClass(vehicleClass);
        trip.setPrice(tariffDto.prices().price());
        trip.setDetails(PriceBreakdown.from(trip, tariffDto));
        trip.setStatus(TripStatus.SEARCHING);

        activeTripCacheService.saveForPassenger(userId, tripId);
        return tripRepository.save(trip);
    }

    @Transactional
    public void assignDriver(Long tripId, Long driverId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new TripNotFoundException(tripId));

        try {
            PaymentMethodResponse paymentMethod = paymentServiceClient
                    .getDefaultPaymentMethod(trip.getPassengerId());
            log.info("Payment method verified for passenger {}: {} ****{}",
                    trip.getPassengerId(), paymentMethod.cardBrand(), paymentMethod.lastFour());
        } catch (FeignException.NotFound _) {
            log.warn("No payment method for passenger {}, cancelling trip {}",
                    trip.getPassengerId(), tripId);
            trip.setStatus(TripStatus.CANCELLED);
            tripRepository.save(trip);
            notificationPublisher.publishTripCancelled(
                    trip.getPassengerId(),
                    tripId,
                    "Отсутствует платёжный метод"
            );
            return;
        }

        trip.setStatus(TripStatus.DRIVER_ASSIGNED);
        trip.setDriverId(driverId);
        tripRepository.save(trip);

        log.info("Driver {} assigned to trip {}", driverId, tripId);
        notificationPublisher.publishDriverAssigned(trip.getPassengerId(), tripId, driverId);
        activeTripCacheService.saveForDriver(driverId, tripId);
    }

    @Transactional
    public void startTrip(Long tripId, Long driverId) {
        Trip trip = getTripForDriver(tripId, driverId);
        StatusValidationUtil.assertTripHasStatus(trip, TripStatus.DRIVER_ASSIGNED);

        trip.setStatus(TripStatus.IN_PROGRESS);
        tripRepository.save(trip);

        log.info("Trip {} started by driver {}", tripId, driverId);
        notificationPublisher.publishTripStarted(trip.getPassengerId(), tripId);
    }

    @Transactional
    public void completeTrip(Long tripId, Long driverId) {
        Trip trip = getTripForDriver(tripId, driverId);
        StatusValidationUtil.assertTripHasStatus(trip, TripStatus.IN_PROGRESS);

        trip.setStatus(TripStatus.COMPLETED);
        tripRepository.save(trip);

        log.info("Trip {} completed by driver {}, publishing TripCompletedEvent", tripId, driverId);

        tripEventPublisher.publishTripCompleted(new TripCompletedEvent(
                trip.getId(),
                trip.getPassengerId(),
                trip.getDriverId(),
                trip.getPrice(),
                "rub",
                Instant.now()
        ));

        notificationPublisher.publishTripCompleted(trip.getPassengerId(), tripId, trip.getPrice());
        activeTripCacheService.removeForDriver(driverId);
    }

    @Transactional
    public void cancelTrip(Long tripId, Long passengerId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new TripNotFoundException(tripId));

        StatusValidationUtil.assertTripHasNotStatus(trip, TripStatus.COMPLETED);

        if (!trip.getPassengerId().equals(passengerId)) {
            throw new AccessDeniedException("You're not owner of this trip");
        }

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

        activeTripCacheService.removeForPassenger(trip.getPassengerId());

        if (trip.getDriverId() != null) {
            notificationPublisher.publishTripCancelled(trip.getDriverId(), tripId, "Отменено пассажиром");
            activeTripCacheService.removeForDriver(trip.getDriverId());
        }
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

    @Transactional
    public void cancelTripInternal(Long tripId, String message) {
        log.warn("{} for trip {}", message, tripId);
        tripRepository.findById(tripId).ifPresent(trip -> {
            trip.setStatus(TripStatus.CANCELLED);
            tripRepository.save(trip);
        });
    }

    private Trip getTripForDriver(Long tripId, Long driverId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new TripNotFoundException(tripId));

        if (!driverId.equals(trip.getDriverId())) {
            throw new AccessDeniedException("You are not assigned as a driver for this trip");
        }

        return trip;
    }
}

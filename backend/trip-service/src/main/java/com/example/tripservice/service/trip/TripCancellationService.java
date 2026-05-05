package com.example.tripservice.service.trip;

import com.example.shared.exception.common.AccessDeniedException;
import com.example.tripservice.entity.Trip;
import com.example.tripservice.entity.enums.TripStatus;
import com.example.tripservice.exception.TripNotFoundException;
import com.example.tripservice.messaging.NotificationPublisher;
import com.example.tripservice.repository.TripRepository;
import com.example.tripservice.service.search.DriverResponseSubscriber;
import com.example.tripservice.service.search.OfferCacheService;
import com.example.tripservice.util.StatusValidationUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TripCancellationService {

    private final TripRepository tripRepository;
    private final DriverResponseSubscriber responseSubscriber;
    private final OfferCacheService offerCacheService;
    private final ActiveTripCacheService activeTripCacheService;
    private final NotificationPublisher notificationPublisher;

    @Transactional
    public void cancelTrip(Long tripId, Long passengerId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new TripNotFoundException(tripId));

        StatusValidationUtil.assertTripStatusNotIn(trip, List.of(TripStatus.IN_PROGRESS, TripStatus.COMPLETED));

        if (!trip.getPassengerId().equals(passengerId)) {
            throw new AccessDeniedException("You're not owner of this trip");
        }

        TripStatus previousStatus = trip.getStatus();
        trip.setStatus(TripStatus.CANCELLED);
        tripRepository.save(trip);

        if (previousStatus == TripStatus.SEARCHING) {
            responseSubscriber.cancelFuture(tripId);
            offerCacheService.removeActiveOffer(tripId);
        }

        log.info("Trip {} cancelled by passenger {}", tripId, passengerId);

        activeTripCacheService.removeForPassenger(trip.getPassengerId());

        if (trip.getDriverId() != null) {
            notificationPublisher.publishTripCancelled(trip.getDriverId(), tripId, "Отменено пассажиром");
            activeTripCacheService.removeForDriver(trip.getDriverId());
        }
    }
}

package com.example.tripservice.service.trip;

import com.example.shared.dto.enums.VehicleClass;
import com.example.shared.exception.common.AccessDeniedException;
import com.example.tripservice.client.PaymentServiceClient;
import com.example.tripservice.dto.data.*;
import com.example.shared.dto.request.CreateHoldRequest;
import com.example.tripservice.entity.Trip;
import com.example.tripservice.entity.enums.TripStatus;
import com.example.tripservice.exception.PaymentMethodNotFoundException;
import com.example.tripservice.exception.TripBookingException;
import com.example.tripservice.exception.TripNotFoundException;
import com.example.tripservice.repository.TripRepository;
import com.example.tripservice.service.cache.ActiveTripCacheService;
import com.example.tripservice.service.external.NavigationService;
import com.example.tripservice.service.search.DriverSearchService;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Objects;

import static com.example.tripservice.util.StatusValidationUtil.ACTIVE_STATUSES;

@Service
@RequiredArgsConstructor
@Slf4j
public class TripService {

    private final TripRepository tripRepository;
    private final NavigationService navigationService;
    private final DriverSearchService driverSearchService;
    private final PaymentServiceClient paymentServiceClient;
    private final TripStatusService tripStatusService;
    private final ActiveTripCacheService activeTripCacheService;

    // Метод просто меняет статус поездки и заполняет его данными, сам поиск происходит в DriverService
    public void startSearching(Long userId, Long tripId, VehicleClass vehicleClass) {

        Trip trip = tripStatusService.setSearching(userId, tripId, vehicleClass);

        try {
            paymentServiceClient.createHold(new CreateHoldRequest(
                    tripId,
                    userId,
                    null,
                    null,
                    trip.getPrice(),
                    "rub"
            ));
            log.info("Hold created for trip {}", tripId);

        } catch (FeignException.NotFound _) {
            log.warn("Payment method not found for user {}", userId);
            tripStatusService.cancelTripByPayment(tripId, "Payment failed");
            throw new PaymentMethodNotFoundException(userId);

        } catch (Exception e) {
            log.error("Payment failed for trip {}: {}", tripId, e.getMessage());
            tripStatusService.cancelTripByPayment(tripId, "Payment failed");
            throw new TripBookingException("Payment failed, trip cancelled");
        }
    }

    public void beginDriverSearch(Long tripId, BigDecimal longitude, BigDecimal latitude, VehicleClass vehicleClass) {

        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new TripNotFoundException(tripId));

        driverSearchService.searchDrivers(trip, longitude, latitude, vehicleClass);
    }

    public RouteDto getRouteToPassenger(Long tripId, BigDecimal driverLng, BigDecimal driverLat) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new TripNotFoundException(tripId));

        return navigationService.getRouteInfo(
                driverLng, driverLat,
                trip.getOriginLng(), trip.getOriginLat()
        );
    }

    @Transactional(readOnly = true)
    public TripDto getTripById(Long userId, Long tripId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new TripNotFoundException(tripId));

        if (!Objects.equals(userId, trip.getPassengerId())) {
            throw new AccessDeniedException("You are not owner of this trip");
        }

        return TripDto.from(trip, null, null);
    }

    @Transactional(readOnly = true)
    public TripDto getActiveTripByPassengerId(Long passengerId) {
        Long existingId = activeTripCacheService.getPassengerActiveTripId(passengerId);
        if (existingId == null) return null;

        Trip trip = tripRepository.findById(existingId).orElse(null);

        if (trip == null || !ACTIVE_STATUSES.contains(trip.getStatus())) {
            activeTripCacheService.removeForPassenger(passengerId);
            return null;
        }

        RouteDto route = navigationService.getRouteInfo(
                trip.getOriginLng(), trip.getOriginLat(),
                trip.getDestinationLng(), trip.getDestinationLat()
        );

        return TripDto.from(trip, null, route.geometry());
    }

    @Transactional(readOnly = true)
    public TripDto getActiveTripByDriverId(Long driverId, BigDecimal driverLat, BigDecimal driverLng) {
        Long existingId = activeTripCacheService.getDriverActiveTripId(driverId);
        if (existingId == null) return null;

        Trip trip = tripRepository.findById(existingId).orElse(null);

        if (trip == null || !ACTIVE_STATUSES.contains(trip.getStatus())) {
            activeTripCacheService.removeForDriver(driverId);
            return null;
        }

        // Логика выдачи маршрута
        RouteDto route;
        if (trip.getStatus() == TripStatus.DRIVER_ASSIGNED) {
            // От водителя до пассажира
            route = navigationService.getRouteInfo(
                    driverLng, driverLat,
                    trip.getOriginLng(), trip.getOriginLat()
            );
        } else {
            // Маршрут самой поездки
            route = navigationService.getRouteInfo(
                    trip.getOriginLng(), trip.getOriginLat(),
                    trip.getDestinationLng(), trip.getDestinationLat()
            );
        }

        return TripDto.from(trip, null, route.geometry());
    }
}

package com.example.tripservice.service;

import com.example.shared.dto.enums.VehicleClass;
import com.example.shared.exception.common.AccessDeniedException;
import com.example.tripservice.client.PaymentServiceClient;
import com.example.tripservice.dto.data.*;
import com.example.shared.dto.request.CreateHoldRequest;
import com.example.tripservice.entity.Trip;
import com.example.tripservice.exception.PaymentMethodNotFoundException;
import com.example.tripservice.exception.TripBookingException;
import com.example.tripservice.exception.TripNotFoundException;
import com.example.tripservice.repository.TripRepository;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class TripService {

    private final TripRepository tripRepository;
    private final NavigationService navigationService;
    private final DriverSearchService driverSearchService;
    private final PaymentServiceClient paymentServiceClient;
    private final TripStatusService tripStatusService;

    // Метод просто меняет статус поездки и заполняет его данными, сам поиск происходит в DriverService
    public AddressDto startSearching(Long userId, Long tripId, VehicleClass vehicleClass) {

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
            tripStatusService.cancelTripInternal(tripId, "Payment failed");
            throw new PaymentMethodNotFoundException(userId);

        } catch (Exception e) {
            log.error("Payment failed for trip {}: {}", tripId, e.getMessage());
            tripStatusService.cancelTripInternal(tripId, "Payment failed");
            throw new TripBookingException("Payment failed, trip cancelled");
        }

        return new AddressDto(
                trip.getOriginAddress(),
                trip.getOriginLat(),
                trip.getOriginLng()
        );
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
}

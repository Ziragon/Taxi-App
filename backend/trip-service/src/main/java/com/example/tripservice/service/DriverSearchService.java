package com.example.tripservice.service;

import com.example.shared.dto.data.DriverLocationDto;
import com.example.shared.dto.enums.VehicleClass;
import com.example.shared.exception.common.ServiceUnavailableException;
import com.example.tripservice.client.UserServiceClient;
import com.example.tripservice.entity.Trip;
import com.example.tripservice.exception.TripNotFoundException;
import com.example.tripservice.messaging.TripOfferPublisher;
import com.example.tripservice.repository.TripRepository;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class DriverSearchService {

    private final UserServiceClient locationClient;
    private final TripStatusService tripStatusService;
    private final OfferCacheService offerCacheService;
    private final ProfileStatusService profileStatusService;

    private final DriverResponseSubscriber responseSubscriber;
    private final DriverResponsePublisher responsePublisher;
    private final TripOfferPublisher tripOfferPublisher;
    private final TripRepository tripRepository;

    @Value("${searching.duration}")
    private int searchDuration;
    @Value("${searching.radiuses}")
    private int[] radiuses;

    public List<DriverLocationDto> getNearbyDrivers(BigDecimal longitude, BigDecimal latitude, BigDecimal radius) {
        try {
            return locationClient.getNearbyDrivers(longitude, latitude, radius, null);
        } catch (FeignException e) {
            log.error("Failed to fetch nearby drivers: {}", e.getMessage());
            throw new ServiceUnavailableException("User-service");
        }
    }

    public List<DriverLocationDto> getNearbyDrivers(BigDecimal longitude, BigDecimal latitude, BigDecimal radius, VehicleClass vehicleClass) {
        try {
            return locationClient.getNearbyDrivers(longitude, latitude, radius, vehicleClass);
        } catch (FeignException e) {
            log.error("Failed to fetch nearby drivers with class: {}", e.getMessage());
            throw new ServiceUnavailableException("User-service");
        }
    }

    @Async
    public void searchDrivers(Long tripId, BigDecimal longitude, BigDecimal latitude, VehicleClass vehicleClass) {

        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new TripNotFoundException(tripId));

        Set<Long> alreadyOffered = new HashSet<>();
        CompletableFuture<Long> future = new CompletableFuture<>();
        responseSubscriber.registerFuture(tripId, future);

        try {
            for (int radius : radiuses) {
                log.debug("Driver searching with radius {}", radius);

                List<DriverLocationDto> drivers = getNearbyDrivers(
                        longitude, latitude, BigDecimal.valueOf(radius), vehicleClass)
                        .stream()
                        .filter(d -> !alreadyOffered.contains(d.driverId()))
                        .toList();
                log.info(drivers.toString());
                for (DriverLocationDto driver : drivers) {
                    alreadyOffered.add(driver.driverId());

                    if (future.isCompletedExceptionally()) {
                        future = new CompletableFuture<>();
                        responseSubscriber.registerFuture(tripId, future);
                    }

                    offerCacheService.setActiveOffer(tripId, driver.driverId());

                    tripOfferPublisher.publishOffer(trip, driver.driverId());

                    boolean accepted = waitForAccept(future);
                    offerCacheService.removeActiveOffer(tripId);

                    if (accepted) {
                        Long driverId = future.getNow(null);
                        tripStatusService.assignDriver(tripId, driverId);
                        return;
                    }

                    tripOfferPublisher.publishOfferExpired(tripId, driver.driverId());
                }
            }

            log.info("Drivers for trip {} not found", tripId);
            tripStatusService.cancelSearch(tripId);

        } finally {
            responseSubscriber.removeFuture(tripId);
        }
    }

    public void handleDriverAccept(Long tripId, Long driverId) {
        profileStatusService.verifyDriverCanDrive(driverId);
        offerCacheService.validateActiveOffer(tripId, driverId);
        responsePublisher.publish(tripId, "ACCEPT", driverId);
        profileStatusService.setDriverStatusBusy(driverId);
    }

    // В будущем желательно сделать какой-либо штраф и тд.
    public void handleDriverReject(Long tripId, Long driverId) {
        offerCacheService.validateActiveOffer(tripId, driverId);
        log.info("Driver {} rejected offer for trip {}", driverId, tripId);
        responsePublisher.publish(tripId, "REJECT", driverId);
    }

    private boolean waitForAccept(CompletableFuture<Long> future) {
        try {
            future.get(searchDuration, TimeUnit.SECONDS);
            return true;
        } catch (TimeoutException _) {
            return false;
        } catch (ExecutionException _) {
            log.debug("Driver rejected offer for trip, moving to next driver");
            return false;
        } catch (InterruptedException _) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
}

package com.example.tripservice.service.search;

import com.example.shared.dto.data.DriverLocationDto;
import com.example.shared.dto.enums.VehicleClass;
import com.example.shared.exception.common.ServiceUnavailableException;
import com.example.tripservice.client.UserServiceClient;
import com.example.tripservice.dto.data.DriverResponseDto;
import com.example.tripservice.entity.Trip;
import com.example.tripservice.entity.enums.DriverReply;
import com.example.tripservice.messaging.TripOfferPublisher;
import com.example.tripservice.service.external.ProfileStatusService;
import com.example.tripservice.service.trip.TripStatusService;
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
    public void searchDrivers(Trip trip, BigDecimal longitude, BigDecimal latitude, VehicleClass vehicleClass) {
        Set<Long> alreadyOffered = new HashSet<>();

        try {
            for (int radius : radiuses) {
                log.debug("Driver searching with radius {}", radius);

                List<DriverLocationDto> drivers = getNearbyDrivers(
                        longitude, latitude, BigDecimal.valueOf(radius), vehicleClass)
                        .stream()
                        .filter(d -> !alreadyOffered.contains(d.driverId()))
                        .toList();

                for (DriverLocationDto driver : drivers) {
                    alreadyOffered.add(driver.driverId());

                    CompletableFuture<DriverResponseDto> future = new CompletableFuture<DriverResponseDto>()
                            .completeOnTimeout(DriverResponseDto.timeout(), searchDuration, TimeUnit.SECONDS);

                    responseSubscriber.registerFuture(trip.getId(), future);

                    try {
                        offerCacheService.setActiveOffer(trip.getId(), driver.driverId());
                        tripOfferPublisher.publishOffer(trip, driver.driverId());

                        DriverResponseDto response = future.join();

                        if (response.type() == DriverReply.ACCEPT) {
                            log.info("Driver {} accepted trip {}", response.driverId(), trip.getId());
                            tripStatusService.assignDriver(trip.getId(), response.driverId());
                            return;
                        }

                        if (response.type() == DriverReply.TIMEOUT) {
                            log.debug("Driver {} timed out for trip {}", driver.driverId(), trip.getId());
                            tripOfferPublisher.publishOfferExpired(trip.getId(), driver.driverId());
                        } else {
                            log.debug("Driver {} rejected trip {}", driver.driverId(), trip.getId());
                        }

                    } finally {
                        responseSubscriber.removeFuture(trip.getId());
                        offerCacheService.removeActiveOffer(trip.getId());
                    }
                }
            }

            log.info("Drivers for trip {} not found after all radiuses", trip.getId());
            tripStatusService.cancelSearch(trip.getId());

        } catch (Exception e) {
            log.error("Global error during driver search for trip {}", trip.getId(), e);
        }
    }

    public void handleDriverAccept(Long tripId, Long driverId) {
        profileStatusService.verifyDriverCanDrive(driverId);
        offerCacheService.validateAndRemoveActiveOffer(tripId, driverId);
        responsePublisher.publish(tripId, "ACCEPT", driverId);
        profileStatusService.setDriverStatusBusy(driverId);
    }

    // В будущем желательно сделать какой-либо штраф и тд.
    public void handleDriverReject(Long tripId, Long driverId) {
        offerCacheService.validateAndRemoveActiveOffer(tripId, driverId);
        log.info("Driver {} rejected offer for trip {}", driverId, tripId);
        responsePublisher.publish(tripId, "REJECT", driverId);
    }
}

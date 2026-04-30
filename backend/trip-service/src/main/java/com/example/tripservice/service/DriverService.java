package com.example.tripservice.service;

import com.example.shared.dto.data.DriverLocationDto;
import com.example.shared.dto.enums.VehicleClass;
import com.example.shared.exception.common.ServiceUnavailableException;
import com.example.tripservice.client.DriverLocationClient;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class DriverService {

    private final DriverLocationClient locationClient;
    private final TripStatusService tripStatusService;
    private final Map<Long, CompletableFuture<Long>> pendingOffers = new ConcurrentHashMap<>();
    private static final int SEARCH_DURATION = 15;
    private static final int[] radiuses = {10, 20, 30};


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

        Set<Long> alreadyOffered = new HashSet<>();

        CompletableFuture<Long> future = new CompletableFuture<>();
        pendingOffers.put(tripId, future);

        try {
            for (int radius : radiuses) {
                List<DriverLocationDto> drivers = getNearbyDrivers(longitude, latitude, BigDecimal.valueOf(radius), vehicleClass)
                        .stream()
                        .filter(d -> !alreadyOffered.contains(d.driverId()))
                        .toList();

                for (DriverLocationDto driver : drivers) {
                    alreadyOffered.add(driver.driverId());

                    if (future.isCompletedExceptionally()) {
                        future = new CompletableFuture<>();
                        pendingOffers.put(tripId, future);
                    }

                    // TODO: Отправка оффера водителю

                    boolean accepted = waitForAccept(future);
                    if (accepted) {
                        Long driverId = future.getNow(null);
                        tripStatusService.assignDriver(tripId, driverId);
                        return;
                    }

                    // TODO: Уведомление, что предложение истекло
                }
            }

            tripStatusService.cancelSearch(tripId);

        } finally {
            pendingOffers.remove(tripId);
        }
    }

    private boolean waitForAccept(CompletableFuture<Long> future) {
        try {
            future.get(SEARCH_DURATION, TimeUnit.SECONDS);
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

    public void handleDriverAccept(Long tripId, Long driverId) {
        CompletableFuture<Long> future = pendingOffers.get(tripId);
        if (future != null) {
            future.complete(driverId);
        }
    }

    public void handleDriverReject(Long tripId, Long driverId) {
        log.info("Driver {} rejected offer for trip {}", driverId, tripId);
        CompletableFuture<Long> future = pendingOffers.get(tripId);
        if (future != null) {
            future.completeExceptionally(new CancellationException());
        }
    }
}

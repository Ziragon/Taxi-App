package com.example.tripservice.service.external;

import com.example.shared.exception.common.ServiceUnavailableException;
import com.example.tripservice.dto.data.TripCreateDto;
import com.example.tripservice.dto.data.TripExternalDto;
import com.example.tripservice.exception.TripBookingException;
import com.example.tripservice.service.pricing.TariffService;
import com.example.tripservice.service.search.DriverSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

@Service
@RequiredArgsConstructor
@Slf4j
public class TripDataAggregator {

    private final WeatherService weatherService;
    private final NavigationService navigationService;
    private final DriverSearchService driverSearchService;
    private final TariffService tariffService;

    @Qualifier("applicationTaskExecutor")
    private final AsyncTaskExecutor executor;

    public TripExternalDto fetchAll(TripCreateDto dto) {
        var routeFuture = CompletableFuture.supplyAsync(() ->
                        navigationService.getRouteInfo(
                                dto.originLng(), dto.originLat(),
                                dto.destLng(), dto.destLat()),
                executor);

        var weatherFuture = CompletableFuture.supplyAsync(() ->
                        weatherService.getWeatherCoef(dto.originLng(), dto.originLat()),
                executor);

        var driversFuture = CompletableFuture.supplyAsync(() ->
                        driverSearchService.getNearbyDrivers(
                                dto.originLng(), dto.originLat(), new BigDecimal("15")),
                executor);

        var tariffsFuture = CompletableFuture.supplyAsync(
                tariffService::getActiveTariffs, executor);

        awaitAll(routeFuture, weatherFuture, driversFuture, tariffsFuture);

        return new TripExternalDto(
                getOrThrow(routeFuture, "navigation"),
                getOrThrow(weatherFuture, "weather"),
                getOrThrow(driversFuture, "drivers"),
                getOrThrow(tariffsFuture, "tariffs")
        );
    }

    private void awaitAll(CompletableFuture<?>... futures) {
        try {
            CompletableFuture.allOf(futures).join();
        } catch (CompletionException ex) {
            Throwable cause = ex.getCause();
            log.error("External data fetch failed: {}", cause.getMessage());
            if (cause instanceof ServiceUnavailableException) {
                throw (ServiceUnavailableException) cause;
            }
            throw new TripBookingException("Failed to gather trip data: " + cause.getMessage());
        }
    }

    private <T> T getOrThrow(CompletableFuture<T> future, String source) {
        try {
            return future.join();
        } catch (CompletionException ex) {
            log.error("Failed to get result from {}: {}", source, ex.getMessage());
            throw new TripBookingException("Failed to get data from " + source);
        }
    }
}
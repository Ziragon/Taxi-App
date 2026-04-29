package com.example.tripservice.service;

import com.example.shared.dto.data.DriverLocationDto;
import com.example.shared.dto.enums.VehicleClass;
import com.example.tripservice.dto.data.*;
import com.example.tripservice.entity.Trip;
import com.example.tripservice.entity.enums.TripStatus;
import com.example.tripservice.repository.TripRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TripService {

    private final TripRepository tripRepository;
    private final WeatherService weatherService;
    private final NavigationService navigationService;
    private final PriceService priceService;
    private final TariffService tariffService;
    private final DriverService driverService;
    @Qualifier("applicationTaskExecutor")
    private final AsyncTaskExecutor executor;

    @Transactional
    public TripDto createTrip(Long userId, TripCreateDto dto) {

        Trip trip = Trip.builder()
                .passengerId(userId)
                .status(TripStatus.CREATED)
                .originAddress(dto.originAddress())
                .originLat(dto.originLat())
                .originLng(dto.originLng())
                .destinationAddress(dto.destAddress())
                .destinationLat(dto.destLat())
                .destinationLng(dto.destLng())
                .build();

        var routeFuture = CompletableFuture.supplyAsync(() ->
                navigationService.getRouteInfo(dto.originLng(), dto.originLat(), dto.destLng(), dto.destLat()),
                executor);

        var weatherFuture = CompletableFuture.supplyAsync(() ->
                weatherService.getWeatherCoef(dto.originLng(), dto.originLat()),
                executor);

        var driversFuture = CompletableFuture.supplyAsync(() ->
                driverService.getNearbyDrivers(dto.originLng(), dto.originLat(), new BigDecimal("60")),
                executor);

        CompletableFuture.allOf(routeFuture, weatherFuture, driversFuture).join();

        RouteDto route = routeFuture.join();
        WeatherDto weather = weatherFuture.join();
        List<DriverLocationDto> drivers = driversFuture.join();

        Map<VehicleClass, Long> driverCountByClass = drivers.stream()
                .collect(Collectors.groupingBy(
                        DriverLocationDto::vehicleClass,
                        Collectors.counting()
                ));

        trip.setDistanceKm(BigDecimal.valueOf(route.distance()).divide(new BigDecimal("1000"), 3, RoundingMode.HALF_UP));
        trip.setDurationMin(BigDecimal.valueOf(route.duration()).divide(new BigDecimal("60"), 10, RoundingMode.HALF_UP));
        trip.setWeatherCoef(weather.weatherCoef());
        trip.setSurgeCoef(priceService.getSurgeCoef(weather.localtime()));

        List<TariffDto> tariffDtos = tariffService.calculateAllTariffs(TripDto.from(trip, null));

        List<TariffDto> filteredTariffs = tariffDtos.stream()
                .filter(t -> driverCountByClass.containsKey(t.tripClass()))
                .map(t -> {
                    int count = driverCountByClass.getOrDefault(t.tripClass(), 0L).intValue();

                    return new TariffDto(
                            t.id(),
                            t.tripClass(),
                            t.baseFare(),
                            t.pricePerKm(),
                            t.pricePerMin(),
                            t.prices(),
                            count
                    );
                })
                .toList();

        Trip saved = tripRepository.save(trip);

        return TripDto.from(saved, filteredTariffs);
    }
}

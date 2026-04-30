package com.example.tripservice.service;

import com.example.shared.dto.data.DriverLocationDto;
import com.example.shared.dto.enums.VehicleClass;
import com.example.shared.exception.common.AccessDeniedException;
import com.example.tripservice.dto.data.*;
import com.example.tripservice.entity.Tariff;
import com.example.tripservice.entity.Trip;
import com.example.tripservice.entity.enums.TripStatus;
import com.example.tripservice.exception.TripNotFoundException;
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

        Trip trip = buildTrip(userId, dto);

        var routeFuture = CompletableFuture.supplyAsync(() ->
                navigationService.getRouteInfo(dto.originLng(), dto.originLat(), dto.destLng(), dto.destLat()),
                executor);

        var weatherFuture = CompletableFuture.supplyAsync(() ->
                weatherService.getWeatherCoef(dto.originLng(), dto.originLat()),
                executor);

        var driversFuture = CompletableFuture.supplyAsync(() ->
                driverService.getNearbyDrivers(dto.originLng(), dto.originLat(), new BigDecimal("30")),
                executor);

        var tariffsFuture = CompletableFuture.supplyAsync(
                tariffService::getActiveTariffs,
                executor
        );

        CompletableFuture.allOf(routeFuture, weatherFuture, driversFuture, tariffsFuture).join();

        RouteDto route = routeFuture.join();
        WeatherDto weather = weatherFuture.join();
        List<DriverLocationDto> drivers = driversFuture.join();
        List<Tariff> tariffs = tariffsFuture.join();

        trip.setDistanceKm(BigDecimal.valueOf(route.distance()).divide(new BigDecimal("1000"), 3, RoundingMode.HALF_UP));
        trip.setDurationMin(BigDecimal.valueOf(route.duration()).divide(new BigDecimal("60"), 10, RoundingMode.HALF_UP));
        trip.setWeatherCoef(weather.weatherCoef());
        trip.setSurgeCoef(priceService.getSurgeCoef(weather.localtime()));

        List<TariffDto> tariffDtos = buildFilteredTariffs(tariffs, drivers, TripDto.from(trip, null, null));

        Trip saved = tripRepository.save(trip);

        return TripDto.from(saved, tariffDtos, route.geometry());
    }

    // TripService
    @Transactional
    public AddressDto startSearching(Long userId, Long tripId, VehicleClass vehicleClass) {

        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new TripNotFoundException(tripId));

        if (!trip.getPassengerId().equals(userId)) {
            throw new AccessDeniedException();
        }

        Tariff tariff = tariffService.getByVehicleClass(vehicleClass);
        TariffDto tariffDto = tariffService.calculatePrice(tariff, TripDto.from(trip, null, null));

        trip.setTripClass(vehicleClass);
        trip.setPrice(tariffDto.prices().price());
        trip.setDetails(PriceBreakdown.from(trip, tariffDto));
        trip.setStatus(TripStatus.SEARCHING);

        tripRepository.save(trip);
        return new AddressDto(
                trip.getOriginAddress(),
                trip.getOriginLat(),
                trip.getOriginLng()
        );
    }

    public void beginDriverSearch(Long tripId, BigDecimal longitude, BigDecimal latitude, VehicleClass vehicleClass) {
        driverService.searchDrivers(tripId, longitude, latitude, vehicleClass);
    }

    private Trip buildTrip(Long userId, TripCreateDto dto) {
        return Trip.builder()
                .passengerId(userId)
                .status(TripStatus.CREATED)
                .originAddress(dto.originAddress())
                .originLat(dto.originLat())
                .originLng(dto.originLng())
                .destinationAddress(dto.destAddress())
                .destinationLat(dto.destLat())
                .destinationLng(dto.destLng())
                .build();
    }

    private List<TariffDto> buildFilteredTariffs(List<Tariff> tariffs,
                                                 List<DriverLocationDto> drivers,
                                                 TripDto tripDto) {
        Map<VehicleClass, Long> driverCountByClass = drivers.stream()
                .collect(Collectors.groupingBy(DriverLocationDto::vehicleClass, Collectors.counting()));

        return tariffs.stream()
                .filter(t -> driverCountByClass.containsKey(t.getTripClass()))
                .map(t -> tariffService.calculatePrice(t, tripDto))
                .map(t -> new TariffDto(
                        t.id(),
                        t.tripClass(),
                        t.baseFare(),
                        t.pricePerKm(),
                        t.pricePerMin(),
                        t.prices(),
                        driverCountByClass.get(t.tripClass()).intValue()
                ))
                .toList();
    }
}

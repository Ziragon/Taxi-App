package com.example.tripservice.service.trip;

import com.example.shared.dto.data.DriverLocationDto;
import com.example.shared.dto.enums.VehicleClass;
import com.example.tripservice.dto.data.*;
import com.example.tripservice.entity.Tariff;
import com.example.tripservice.entity.Trip;
import com.example.tripservice.entity.enums.TripStatus;
import com.example.tripservice.exception.TripAlreadyExistsException;
import com.example.tripservice.exception.TripBookingException;
import com.example.tripservice.repository.TripRepository;
import com.example.tripservice.service.external.NavigationService;
import com.example.tripservice.service.external.ProfileStatusService;
import com.example.tripservice.service.external.WeatherService;
import com.example.tripservice.service.pricing.PriceService;
import com.example.tripservice.service.pricing.TariffService;
import com.example.tripservice.service.search.DriverSearchService;
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
public class TripCreationService {

    private final TripRepository tripRepository;
    private final WeatherService weatherService;
    private final NavigationService navigationService;
    private final PriceService priceService;
    private final TariffService tariffService;
    private final DriverSearchService driverSearchService;
    private final ProfileStatusService profileStatusService;
    @Qualifier("applicationTaskExecutor")
    private final AsyncTaskExecutor executor;

    private static final List<TripStatus> ACTIVE_STATUSES = List.of(
            TripStatus.SEARCHING, TripStatus.DRIVER_ASSIGNED, TripStatus.IN_PROGRESS
    );

    @Transactional
    public TripDto createTrip(Long userId, TripCreateDto dto) {

        profileStatusService.verifyPassengerCanOrder(userId);

        boolean hasActive = tripRepository.existsByPassengerIdAndStatusIn(userId, ACTIVE_STATUSES);
        if (hasActive) throw new TripAlreadyExistsException();

        // Для уменьшения мусора в бд используем уже созданную поездку пользователя
        // Если нет - создаем новую
        Trip trip = tripRepository.findFirstByPassengerIdAndStatusOrderByCreatedAtDesc(userId, TripStatus.CREATED)
                .orElseGet(() -> buildTrip(userId, dto));

        Trip updatedTrip = updateTripCoordinates(trip, dto);

        TripExternalDto data = fetchExternalData(dto);

        updatedTrip.setDistanceKm(BigDecimal.valueOf(data.route().distance()).divide(new BigDecimal("1000"), 3, RoundingMode.HALF_UP));
        updatedTrip.setDurationMin(BigDecimal.valueOf(data.route().duration()).divide(new BigDecimal("60"), 2, RoundingMode.HALF_UP));
        updatedTrip.setWeatherCoef(data.weather().weatherCoef());
        updatedTrip.setSurgeCoef(priceService.getSurgeCoef(data.weather().localtime()));

        List<TariffDto> tariffDtos = buildFilteredTariffs(data.tariffs(), data.drivers(), TripDto.from(updatedTrip, null, null));

        Trip saved = tripRepository.save(updatedTrip);

        return TripDto.from(saved, tariffDtos, data.route().geometry());
    }

    private TripExternalDto fetchExternalData(TripCreateDto dto) {

        var routeFuture = CompletableFuture.supplyAsync(() ->
                        navigationService.getRouteInfo(dto.originLng(), dto.originLat(), dto.destLng(), dto.destLat()),
                executor);

        var weatherFuture = CompletableFuture.supplyAsync(() ->
                        weatherService.getWeatherCoef(dto.originLng(), dto.originLat()),
                executor);

        var driversFuture = CompletableFuture.supplyAsync(() ->
                        driverSearchService.getNearbyDrivers(dto.originLng(), dto.originLat(), new BigDecimal("30")),
                executor);

        var tariffsFuture = CompletableFuture.supplyAsync(
                tariffService::getActiveTariffs,
                executor
        );

        CompletableFuture.allOf(routeFuture, weatherFuture, driversFuture, tariffsFuture)
                .exceptionally(ex -> {
                    log.error("Failed to fetch trip data", ex);
                    throw new TripBookingException("Failed to gather trip data" + ex.getMessage());
                })
                .join();

        RouteDto route = routeFuture.join();
        WeatherDto weather = weatherFuture.join();
        List<DriverLocationDto> drivers = driversFuture.join();
        List<Tariff> tariffs = tariffsFuture.join();

        return new TripExternalDto(
                route,
                weather,
                drivers,
                tariffs
        );
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

    private Trip updateTripCoordinates(Trip trip, TripCreateDto dto) {
        trip.setOriginAddress(dto.originAddress());
        trip.setOriginLat(dto.originLat());
        trip.setOriginLng(dto.originLng());
        trip.setDestinationAddress(dto.destAddress());
        trip.setDestinationLat(dto.destLat());
        trip.setDestinationLng(dto.destLng());
        return trip;
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
}
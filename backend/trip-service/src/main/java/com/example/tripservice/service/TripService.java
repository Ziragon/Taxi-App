package com.example.tripservice.service;

import com.example.tripservice.dto.data.*;
import com.example.tripservice.entity.Trip;
import com.example.tripservice.entity.enums.TripStatus;
import com.example.tripservice.repository.TripRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TripService {

    private final TripRepository tripRepository;
    private final WeatherService weatherService;
    private final NavigationService navigationService;
    private final PriceService priceService;
    private final TariffService tariffService;

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

        RouteDto route = navigationService.getRouteInfo(dto.originLng(), dto.originLat(), dto.destLng(), dto.destLat());
        WeatherDto weather = weatherService.getWeatherCoef(dto.originLng(), dto.originLat());

        trip.setDistanceKm(BigDecimal.valueOf(route.distance()).divide(new BigDecimal("1000"), 3, RoundingMode.HALF_UP));
        trip.setDurationMin(BigDecimal.valueOf(route.duration()).divide(new BigDecimal("60"), 10, RoundingMode.HALF_UP));
        trip.setWeatherCoef(weather.weatherCoef());
        trip.setSurgeCoef(priceService.getSurgeCoef(weather.localtime()));

        List<TariffDto> tariffDtos = tariffService.calculateAllTariffs(TripDto.from(trip, null));

        Trip saved = tripRepository.save(trip);

        return TripDto.from(saved, tariffDtos);
    }
}

package com.example.tripservice.service;

import com.example.tripservice.dto.data.RouteDto;
import com.example.tripservice.dto.data.TripCreateDto;
import com.example.tripservice.dto.data.TripDto;
import com.example.tripservice.dto.data.WeatherDto;
import com.example.tripservice.entity.Trip;
import com.example.tripservice.entity.enums.TripStatus;
import com.example.tripservice.repository.TripRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class TripService {

    private final TripRepository tripRepository;
    private final WeatherService weatherService;
    private final NavigationService navigationService;

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

        trip.setDistanceKm(BigDecimal.valueOf(route.distance() / 1000));
        trip.setDurationSec(route.duration());
        trip.setWeatherCoef(weather.weatherCoef());
        trip.setSurgeCoef(new BigDecimal("1.1"));

        Trip saved = tripRepository.save(trip);
        return TripDto.from(saved);
    }
}

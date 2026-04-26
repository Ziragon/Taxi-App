package com.example.tripservice.service;

import com.example.tripservice.client.OsrmClient;
import com.example.tripservice.dto.data.TripCreateDto;
import com.example.tripservice.dto.data.TripDto;
import com.example.tripservice.dto.response.OsrmResponse;
import com.example.tripservice.entity.Trip;
import com.example.tripservice.entity.enums.TripStatus;
import com.example.tripservice.repository.TripRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class TripService {

    private final TripRepository tripRepository;
    private final OsrmClient osrmClient;

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

        String coords = dto.originLng() + "," + dto.originLat() + ";" + dto.destLng() + "," + dto.destLat();
        OsrmResponse response = osrmClient.getRoute(coords, "full");

        trip.setDistanceKm(BigDecimal.valueOf(response.routes().getFirst().distance() / 1000));
        trip.setDurationSec(response.routes().getFirst().duration());
        trip.setWeatherCoef(new BigDecimal("1.1"));
        trip.setSurgeCoef(new BigDecimal("1.1"));

        Trip saved = tripRepository.save(trip);
        return TripDto.from(saved);
    }
}

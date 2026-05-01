package com.example.tripservice.service;

import com.example.tripservice.entity.Trip;
import com.example.tripservice.entity.enums.TripStatus;
import com.example.tripservice.exception.TripNotFoundException;
import com.example.tripservice.repository.TripRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class TripStatusService {

    private final TripRepository tripRepository;

    @Transactional
    public void assignDriver(Long tripId, Long driverId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new TripNotFoundException(tripId));

        trip.setStatus(TripStatus.DRIVER_ASSIGNED);
        trip.setDriverId(driverId);
        tripRepository.save(trip);

        // TODO - WebSocket уведомление пассажиру
        // Просчет OSRM-маршрута от водителя до пассажира и выдача результата обоим
    }

    @Transactional
    public void cancelSearch(Long tripId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new TripNotFoundException(tripId));

        trip.setStatus(TripStatus.CANCELLED);
        tripRepository.save(trip);

        // TODO - WebSocket уведомление пассажиру
        // Просьба перезаказать
    }
}

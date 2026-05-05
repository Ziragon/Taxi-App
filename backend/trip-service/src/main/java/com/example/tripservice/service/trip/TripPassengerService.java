package com.example.tripservice.service.trip;

import com.example.shared.dto.enums.VehicleClass;
import com.example.tripservice.dto.data.TripCreateDto;
import com.example.tripservice.dto.data.TripDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class TripPassengerService {

    private final TripCreationService tripCreationService;
    private final TripCancellationService tripCancellationService;
    private final TripService tripService;

    public TripDto createTrip(Long userId, TripCreateDto dto) {
        return tripCreationService.createDraft(userId, dto);
    }

    public TripDto startSearching(Long userId, VehicleClass vehicleClass) {
        TripDto trip = tripCreationService.confirmTrip(userId, vehicleClass);
        tripService.startSearching(userId, trip.id(), vehicleClass);
        tripService.beginDriverSearch(trip.id(), trip.originLng(), trip.originLat(), vehicleClass);
        return trip;
    }

    public void cancelTrip(Long tripId, Long passengerId) {
        tripCancellationService.cancelTrip(tripId, passengerId);
    }

    @Transactional(readOnly = true)
    public TripDto getTrip(Long userId, Long tripId) {
        return tripService.getTripById(userId, tripId);
    }
}
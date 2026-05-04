package com.example.tripservice.service.trip;

import com.example.shared.dto.enums.VehicleClass;
import com.example.tripservice.dto.data.AddressDto;
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
    private final TripStatusService tripStatusService;
    private final TripService tripService;

    public TripDto createTrip(Long userId, TripCreateDto dto) {
        return tripCreationService.createTrip(userId, dto);
    }

    public void startSearching(Long userId, Long tripId, VehicleClass vehicleClass) {
        AddressDto dto = tripService.startSearching(userId, tripId, vehicleClass);
        tripService.beginDriverSearch(tripId, dto.longitude(), dto.latitude(), vehicleClass);
    }

    public void cancelTrip(Long tripId, Long passengerId) {
        tripStatusService.cancelTrip(tripId, passengerId);
    }

    @Transactional(readOnly = true)
    public TripDto getTrip(Long userId, Long tripId) {
        return tripService.getTripById(userId, tripId);
    }
}
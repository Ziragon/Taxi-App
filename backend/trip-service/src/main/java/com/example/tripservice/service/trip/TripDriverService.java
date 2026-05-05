package com.example.tripservice.service.trip;

import com.example.tripservice.dto.data.RouteDto;
import com.example.tripservice.dto.data.TripDto;
import com.example.tripservice.service.search.DriverSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class TripDriverService {

    private final TripStatusService tripStatusService;
    private final TripService tripService;
    private final DriverSearchService driverSearchService;

    public RouteDto acceptTrip(Long tripId, Long driverId, BigDecimal lng, BigDecimal lat) {
        driverSearchService.handleDriverAccept(tripId, driverId);
        return tripService.getRouteToPassenger(tripId, lng, lat);
    }

    public void rejectTrip(Long tripId, Long driverId) {
        driverSearchService.handleDriverReject(tripId, driverId);
    }

    public RouteDto startTrip(Long tripId, Long driverId) {
        tripStatusService.startTrip(tripId, driverId);
        return tripService.getRouteForTrip(tripId);
    }

    public void completeTrip(Long tripId, Long driverId) {
        tripStatusService.completeTrip(tripId, driverId);
    }

    @Transactional(readOnly = true)
    public TripDto getActiveTrip(Long userId, BigDecimal driverLat, BigDecimal driverLng) {
        return tripService.getActiveTripByDriverId(userId, driverLat, driverLng);
    }
}
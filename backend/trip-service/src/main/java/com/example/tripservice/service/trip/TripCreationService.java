package com.example.tripservice.service.trip;

import com.example.shared.dto.data.DriverLocationDto;
import com.example.shared.dto.enums.VehicleClass;
import com.example.tripservice.dto.data.*;
import com.example.tripservice.entity.Tariff;
import com.example.tripservice.entity.Trip;
import com.example.tripservice.entity.enums.TripStatus;
import com.example.tripservice.repository.TripRepository;
import com.example.tripservice.service.external.ProfileStatusService;
import com.example.tripservice.service.external.TripDataAggregator;
import com.example.tripservice.service.pricing.PriceService;
import com.example.tripservice.service.pricing.TariffService;
import com.example.tripservice.util.StatusValidationUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TripCreationService {

    private final TripRepository tripRepository;
    private final PriceService priceService;
    private final TariffService tariffService;
    private final ProfileStatusService profileStatusService;
    private final ActiveTripCacheService activeTripCacheService;
    private final TripDataAggregator tripDataAggregator;

    @Transactional
    public TripDto createTrip(Long userId, TripCreateDto dto) {
        profileStatusService.verifyPassengerCanOrder(userId);

        Long existing = activeTripCacheService.getPassengerActiveTripId(userId);
        if (existing != null) {
            tripRepository.findById(existing).ifPresent(StatusValidationUtil::assertTripNotActive);
            activeTripCacheService.removeForPassenger(userId);
        }

        // Для уменьшения мусора в бд используем уже созданную поездку пользователя
        // Если нет - создаем новую
        Trip trip = tripRepository.findFirstByPassengerIdAndStatusOrderByCreatedAtDesc(userId, TripStatus.CREATED)
                .orElseGet(() -> buildTrip(userId, dto));

        updateTripCoordinates(trip, dto);

        TripExternalDto data = tripDataAggregator.fetchAll(dto);

        trip.setDistanceKm(BigDecimal.valueOf(data.route().distance()).divide(new BigDecimal("1000"), 3, RoundingMode.HALF_UP));
        trip.setDurationMin(BigDecimal.valueOf(data.route().duration()).divide(new BigDecimal("60"), 2, RoundingMode.HALF_UP));
        trip.setWeatherCoef(data.weather().weatherCoef());
        trip.setSurgeCoef(priceService.getSurgeCoef(data.weather().localtime()));

        List<TariffDto> tariffDtos = buildFilteredTariffs(
                data.tariffs(), data.drivers(), TripDto.from(trip, null, null));

        Trip saved = tripRepository.save(trip);
        return TripDto.from(saved, tariffDtos, data.route().geometry());
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

    private void updateTripCoordinates(Trip trip, TripCreateDto dto) {
        trip.setOriginAddress(dto.originAddress());
        trip.setOriginLat(dto.originLat());
        trip.setOriginLng(dto.originLng());
        trip.setDestinationAddress(dto.destAddress());
        trip.setDestinationLat(dto.destLat());
        trip.setDestinationLng(dto.destLng());
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
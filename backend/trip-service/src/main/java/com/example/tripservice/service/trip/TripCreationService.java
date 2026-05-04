package com.example.tripservice.service.trip;

import com.example.shared.dto.data.DriverLocationDto;
import com.example.shared.dto.enums.VehicleClass;
import com.example.tripservice.dto.data.*;
import com.example.tripservice.entity.Tariff;
import com.example.tripservice.entity.Trip;
import com.example.tripservice.entity.enums.TripStatus;
import com.example.tripservice.exception.TripDraftExpiredException;
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
    private final TripDraftCacheService tripDraftCacheService;

    public TripDto createDraft(Long userId, TripCreateDto dto) {
        profileStatusService.verifyPassengerCanOrder(userId);
        assertNoActiveTrip(userId);

        TripExternalDto data = tripDataAggregator.fetchAll(dto);

        TripDraftDto draft = buildDraft(userId, dto, data);
        tripDraftCacheService.save(userId, draft);

        List<TariffDto> tariffs = buildFilteredTariffs(data.tariffs(), data.drivers(), draft);
        return TripDto.fromDraft(draft, tariffs, data.route().geometry());
    }

    @Transactional
    public TripDto confirmTrip(Long userId, VehicleClass chosenClass) {
        TripDraftDto draft = tripDraftCacheService.get(userId)
                .orElseThrow(TripDraftExpiredException::new);

        Trip trip = buildTripFromDraft(draft, chosenClass);
        Trip saved = tripRepository.save(trip);

        tripDraftCacheService.delete(userId);

        log.info("Trip {} confirmed for passenger {}", saved.getId(), userId);
        return TripDto.from(saved, null, null);
    }

    private List<TariffDto> buildFilteredTariffs(List<Tariff> tariffs,
                                                 List<DriverLocationDto> drivers,
                                                 TripDraftDto tripDto) {
        Map<VehicleClass, Long> driverCountByClass = drivers.stream()
                .collect(Collectors.groupingBy(DriverLocationDto::vehicleClass, Collectors.counting()));

        return tariffs.stream()
                .filter(t -> driverCountByClass.containsKey(t.getTripClass()))
                .map(t -> {
                    int count = driverCountByClass.get(t.getTripClass()).intValue();
                    return tariffService.calculateTariffOffer(t, tripDto, count);
                })
                .toList();
    }

    private void assertNoActiveTrip(Long userId) {
        Long existingId = activeTripCacheService.getPassengerActiveTripId(userId);
        if (existingId != null) {
            tripRepository.findById(existingId)
                    .ifPresent(StatusValidationUtil::assertTripNotActive);
            activeTripCacheService.removeForPassenger(userId);
        }
    }

    private TripDraftDto buildDraft(Long userId, TripCreateDto dto, TripExternalDto data) {
        return new TripDraftDto(
                userId,
                dto.originAddress(),
                dto.originLat(),
                dto.originLng(),
                dto.destAddress(),
                dto.destLat(),
                dto.destLng(),
                BigDecimal.valueOf(data.route().distance())
                        .divide(new BigDecimal("1000"), 3, RoundingMode.HALF_UP),
                BigDecimal.valueOf(data.route().duration())
                        .divide(new BigDecimal("60"), 2, RoundingMode.HALF_UP),
                data.weather().weatherCoef(),
                priceService.getSurgeCoef(data.weather().localtime())
        );
    }

    private Trip buildTripFromDraft(TripDraftDto draft, VehicleClass vehicleClass) {
        return Trip.builder()
                .passengerId(draft.passengerId())
                .status(TripStatus.CREATED)
                .tripClass(vehicleClass)
                .originAddress(draft.originAddress())
                .originLat(draft.originLat())
                .originLng(draft.originLng())
                .destinationAddress(draft.destinationAddress())
                .destinationLat(draft.destinationLat())
                .destinationLng(draft.destinationLng())
                .distanceKm(draft.distanceKm())
                .durationMin(draft.durationMin())
                .weatherCoef(draft.weatherCoef())
                .surgeCoef(draft.surgeCoef())
                .build();
    }
}
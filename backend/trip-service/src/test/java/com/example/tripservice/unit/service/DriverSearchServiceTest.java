package com.example.tripservice.unit.service;

import com.example.shared.dto.enums.VehicleClass;
import com.example.shared.exception.common.ServiceUnavailableException;
import com.example.tripservice.client.UserServiceClient;
import com.example.tripservice.entity.Trip;
import com.example.tripservice.entity.enums.TripStatus;
import com.example.tripservice.repository.TripRepository;
import com.example.tripservice.service.external.ProfileStatusService;
import com.example.tripservice.service.search.DriverResponsePublisher;
import com.example.tripservice.service.search.DriverResponseSubscriber;
import com.example.tripservice.service.search.DriverSearchService;
import com.example.tripservice.service.search.OfferCacheService;
import com.example.tripservice.service.trip.TripStatusService;
import feign.FeignException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DriverSearchServiceTest {

    @Mock
    private UserServiceClient locationClient;
    @Mock
    private TripStatusService tripStatusService;
    @Mock
    private OfferCacheService offerCacheService;
    @Mock
    private ProfileStatusService profileStatusService;
    @Mock
    private DriverResponseSubscriber responseSubscriber;
    @Mock
    private DriverResponsePublisher responsePublisher;
    @Mock
    private TripRepository tripRepository;

    @InjectMocks
    private DriverSearchService driverSearchService;

    @BeforeEach
    void initValues() {
        ReflectionTestUtils.setField(driverSearchService, "radiuses", new int[]{5, 10, 15});
        ReflectionTestUtils.setField(driverSearchService, "searchDuration", 15);
    }

    @Test
    @DisplayName("handleDriverAccept: должен вызвать валидацию и опубликовать принятие")
    void handleDriverAccept_ShouldValidateAndPublish() {
        Long tripId = 1L;
        Long driverId = 100L;

        driverSearchService.handleDriverAccept(tripId, driverId);

        verify(offerCacheService).validateActiveOffer(tripId, driverId);
        verify(responsePublisher).publish(tripId, "ACCEPT", driverId);
    }

    @Test
    @DisplayName("handleDriverReject: должен вызвать валидацию и опубликовать отказ")
    void handleDriverReject_ShouldValidateAndPublish() {
        Long tripId = 1L;
        Long driverId = 100L;

        driverSearchService.handleDriverReject(tripId, driverId);

        verify(offerCacheService).validateActiveOffer(tripId, driverId);
        verify(responsePublisher).publish(tripId, "REJECT", driverId);
    }

    @Test
    @DisplayName("getNearbyDrivers: должен выбрасывать ServiceUnavailableException при ошибке Feign")
    void getNearbyDrivers_ShouldThrowServiceUnavailable_WhenFeignFails() {
        when(locationClient.getNearbyDrivers(any(), any(), any(), any()))
                .thenThrow(mock(FeignException.class));

        assertThrows(ServiceUnavailableException.class, () ->
                driverSearchService.getNearbyDrivers(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.TEN)
        );
    }

    @Test
    @DisplayName("searchDrivers: должен отменить поиск, если водители не найдены")
    void searchDrivers_ShouldCancelSearch_WhenNoDriversFound() {
        Long tripId = 1L;
        Trip mockTrip = Trip.builder()
                .id(tripId)
                .passengerId(10L)
                .status(TripStatus.SEARCHING)
                .originLat(BigDecimal.ZERO)
                .originLng(BigDecimal.ZERO)
                .destinationLat(BigDecimal.ZERO)
                .destinationLng(BigDecimal.ZERO)
                .build();

        when(locationClient.getNearbyDrivers(any(), any(), any(), any())).thenReturn(List.of());

        driverSearchService.searchDrivers(mockTrip, BigDecimal.ZERO, BigDecimal.ZERO, VehicleClass.ECONOMY);

        verify(responseSubscriber).registerFuture(eq(tripId), any());
        verify(tripStatusService).cancelSearch(tripId);
        verify(responseSubscriber).removeFuture(tripId);
    }
}
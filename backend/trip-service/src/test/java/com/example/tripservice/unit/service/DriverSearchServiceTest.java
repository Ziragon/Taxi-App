package com.example.tripservice.unit.service;

import com.example.shared.dto.enums.VehicleClass;
import com.example.shared.exception.common.ServiceUnavailableException;
import com.example.tripservice.client.DriverLocationClient;
import com.example.tripservice.service.*;
import feign.FeignException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DriverSearchServiceTest {

    @Mock
    private DriverLocationClient locationClient;
    @Mock
    private TripStatusService tripStatusService;
    @Mock
    private OfferCacheService offerCacheService;
    @Mock
    private DriverResponseSubscriber responseSubscriber;
    @Mock
    private DriverResponsePublisher responsePublisher;

    @InjectMocks
    private DriverSearchService driverSearchService;

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
        when(locationClient.getNearbyDrivers(any(), any(), any(), any())).thenReturn(List.of());

        driverSearchService.searchDrivers(tripId, BigDecimal.ZERO, BigDecimal.ZERO, VehicleClass.ECONOMY);

        verify(responseSubscriber).registerFuture(eq(tripId), any());
        verify(tripStatusService).cancelSearch(tripId);
        verify(responseSubscriber).removeFuture(tripId);
    }
}
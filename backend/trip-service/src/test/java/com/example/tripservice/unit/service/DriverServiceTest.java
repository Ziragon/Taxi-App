package com.example.tripservice.unit.service;

import com.example.shared.exception.common.AccessDeniedException;
import com.example.tripservice.client.DriverLocationClient;
import com.example.tripservice.service.DriverService;
import com.example.tripservice.service.TripStatusService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class DriverServiceTest {

    @Mock
    private DriverLocationClient locationClient;
    @Mock
    private TripStatusService tripStatusService;

    @InjectMocks
    private DriverService driverService;

    private Map<Long, CompletableFuture<Long>> pendingOffers;
    private Map<Long, Long> activeOffers;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        pendingOffers = (Map<Long, CompletableFuture<Long>>) ReflectionTestUtils.getField(driverService, "pendingOffers");
        activeOffers = (Map<Long, Long>) ReflectionTestUtils.getField(driverService, "activeOffers");
    }

    @Test
    @DisplayName("Должен корректно обработать принятие заказа")
    void handleDriverAccept_ShouldCompleteFuture_WhenDriverMatches() {
        Long tripId = 1L;
        Long driverId = 100L;
        CompletableFuture<Long> future = new CompletableFuture<>();

        pendingOffers.put(tripId, future);
        activeOffers.put(tripId, driverId);

        driverService.handleDriverAccept(tripId, driverId);

        assertTrue(future.isDone());
        assertEquals(driverId, future.join());
    }

    @Test
    @DisplayName("Должен выкинуть AccessDenied при чужом доступе к Trip")
    void handleDriverAccept_ShouldThrowAccessDenied_WhenDriverMismatches() {
        Long tripId = 1L;
        Long actualDriverId = 100L;
        Long wrongDriverId = 999L;

        activeOffers.put(tripId, actualDriverId);

        assertThrows(AccessDeniedException.class, () ->
                driverService.handleDriverAccept(tripId, wrongDriverId)
        );
    }

    @Test
    @DisplayName("Должен корректно обработать отказ от поездки")
    void handleDriverReject_ShouldCompleteExceptionally_WhenDriverMatches() {
        Long tripId = 1L;
        Long driverId = 100L;
        CompletableFuture<Long> future = new CompletableFuture<>();

        pendingOffers.put(tripId, future);
        activeOffers.put(tripId, driverId);

        driverService.handleDriverReject(tripId, driverId);

        assertTrue(future.isCompletedExceptionally());
        assertThrows(CancellationException.class, future::join);
    }
}
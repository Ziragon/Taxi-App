package com.example.tripservice.integration;

import com.example.tripservice.BaseIntegrationTest;
import com.example.tripservice.dto.data.DriverResponseDto;
import com.example.tripservice.service.search.DriverResponsePublisher;
import com.example.tripservice.service.search.DriverResponseSubscriber;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;

class DriverResponseIntegrationTest extends BaseIntegrationTest {

    private static final long AWAIT_SECONDS = 5L;

    @Autowired
    private DriverResponsePublisher publisher;

    @Autowired
    private DriverResponseSubscriber subscriber;

    @Test
    @DisplayName("Publish ACCEPT - Subscriber получает - future завершается с DriverResponseDto.accept")
    void publish_accept_futureCompletedWithDriverId() throws Exception {
        Long tripId   = 100L;
        Long driverId = 42L;

        CompletableFuture<DriverResponseDto> future = new CompletableFuture<>();
        subscriber.registerFuture(tripId, future);

        publisher.publish(tripId, "ACCEPT", driverId);

        DriverResponseDto response = future.get(AWAIT_SECONDS, TimeUnit.SECONDS);

        assertThat(response).isEqualTo(DriverResponseDto.accept(driverId));
    }

    @Test
    @DisplayName("Publish REJECT - Subscriber получает - future завершается с DriverResponseDto.reject()")
    void publish_reject_futureCompletedWithRejectDto() throws Exception {
        Long tripId   = 101L;
        Long driverId = 77L;

        CompletableFuture<DriverResponseDto> future = new CompletableFuture<>();
        subscriber.registerFuture(tripId, future);

        publisher.publish(tripId, "REJECT", driverId);

        DriverResponseDto response = future.get(AWAIT_SECONDS, TimeUnit.SECONDS);

        assertThat(response).isEqualTo(DriverResponseDto.reject());
    }

    @Test
    @DisplayName("Два независимых tripId - каждая future завершается со своим DriverResponseDto")
    void publish_multipleTripIds_eachFutureCompletedIndependently() throws Exception {
        Long tripId1   = 200L; Long driverId1 = 11L;
        Long tripId2   = 201L; Long driverId2 = 22L;

        CompletableFuture<DriverResponseDto> future1 = new CompletableFuture<>();
        CompletableFuture<DriverResponseDto> future2 = new CompletableFuture<>();
        subscriber.registerFuture(tripId1, future1);
        subscriber.registerFuture(tripId2, future2);

        publisher.publish(tripId1, "ACCEPT", driverId1);
        publisher.publish(tripId2, "ACCEPT", driverId2);

        assertThat(future1.get(AWAIT_SECONDS, TimeUnit.SECONDS)).isEqualTo(DriverResponseDto.accept(driverId1));
        assertThat(future2.get(AWAIT_SECONDS, TimeUnit.SECONDS)).isEqualTo(DriverResponseDto.accept(driverId2));
    }

    @Test
    @DisplayName("Publish без зарегистрированной future - нет исключений ни у publisher, ни у subscriber")
    void publish_noFutureRegistered_noException() {
        assertThatCode(() -> publisher.publish(999L, "ACCEPT", 42L))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Publish после removeFuture - сообщение пришло, future не завершается")
    void publish_afterRemoveFuture_futureNotCompleted() throws Exception {
        Long tripId = 300L;
        CompletableFuture<DriverResponseDto> future = new CompletableFuture<>();

        Long sentinelTripId = 301L;
        CompletableFuture<DriverResponseDto> sentinelFuture = new CompletableFuture<>();

        subscriber.registerFuture(tripId, future);
        subscriber.removeFuture(tripId);

        subscriber.registerFuture(sentinelTripId, sentinelFuture);

        publisher.publish(tripId, "ACCEPT", 42L);
        publisher.publish(sentinelTripId, "ACCEPT", 99L);

        sentinelFuture.get(AWAIT_SECONDS, TimeUnit.SECONDS);

        assertThat(future).isNotDone();
    }

    @Test
    @DisplayName("Два ACCEPT подряд для одного tripId - future завершается первым DriverResponseDto")
    void publish_doubleAccept_futureCompletedByFirstDriver() throws Exception {
        Long tripId    = 400L;
        Long driverId1 = 55L;
        Long driverId2 = 66L;

        CompletableFuture<DriverResponseDto> future = new CompletableFuture<>();
        subscriber.registerFuture(tripId, future);

        publisher.publish(tripId, "ACCEPT", driverId1);
        DriverResponseDto result = future.get(AWAIT_SECONDS, TimeUnit.SECONDS);

        assertThatCode(() -> publisher.publish(tripId, "ACCEPT", driverId2))
                .doesNotThrowAnyException();

        assertThat(result).isEqualTo(DriverResponseDto.accept(driverId1));
    }

    @Test
    @DisplayName("Вызов cancelFuture - future завершается с DriverResponseDto.cancelled()")
    void cancelFuture_futureCompletedWithCancelledDto() throws Exception {
        Long tripId = 500L;

        CompletableFuture<DriverResponseDto> future = new CompletableFuture<>();
        subscriber.registerFuture(tripId, future);

        subscriber.cancelFuture(tripId);

        DriverResponseDto response = future.get(AWAIT_SECONDS, TimeUnit.SECONDS);
        assertThat(response).isEqualTo(DriverResponseDto.cancelled());
    }
}
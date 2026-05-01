package com.example.tripservice.integration;

import com.example.tripservice.BaseIntegrationTest;
import com.example.tripservice.service.DriverResponsePublisher;
import com.example.tripservice.service.DriverResponseSubscriber;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;

class DriverResponseIntegrationTest extends BaseIntegrationTest {

    private static final long AWAIT_SECONDS = 5L;
    private static final long NO_COMPLETE_WAIT_MS = 500L;

    @Autowired
    private DriverResponsePublisher publisher;

    @Autowired
    private DriverResponseSubscriber subscriber;

    @BeforeEach
    void setUp() {
        clearCaches();
    }

    @Test
    @DisplayName("Publish ACCEPT - Subscriber получает - future завершается с driverId")
    void publish_accept_futureCompletedWithDriverId() throws Exception {
        Long tripId   = 100L;
        Long driverId = 42L;

        CompletableFuture<Long> future = new CompletableFuture<>();
        subscriber.registerFuture(tripId, future);

        publisher.publish(tripId, "ACCEPT", driverId);

        assertThat(future.get(AWAIT_SECONDS, TimeUnit.SECONDS)).isEqualTo(driverId);
    }

    @Test
    @DisplayName("Publish REJECT - Subscriber получает - future завершается с CancellationException")
    void publish_reject_futureCompletedExceptionally() {
        Long tripId   = 101L;
        Long driverId = 77L;

        CompletableFuture<Long> future = new CompletableFuture<>();
        subscriber.registerFuture(tripId, future);

        publisher.publish(tripId, "REJECT", driverId);

        assertThatThrownBy(() -> future.get(AWAIT_SECONDS, TimeUnit.SECONDS))
                .isInstanceOf(CancellationException.class);
    }

    @Test
    @DisplayName("Два независимых tripId - каждая future завершается со своим driverId")
    void publish_multipleTripIds_eachFutureCompletedIndependently() throws Exception {
        Long tripId1   = 200L; Long driverId1 = 11L;
        Long tripId2   = 201L; Long driverId2 = 22L;

        CompletableFuture<Long> future1 = new CompletableFuture<>();
        CompletableFuture<Long> future2 = new CompletableFuture<>();
        subscriber.registerFuture(tripId1, future1);
        subscriber.registerFuture(tripId2, future2);

        publisher.publish(tripId1, "ACCEPT", driverId1);
        publisher.publish(tripId2, "ACCEPT", driverId2);

        assertThat(future1.get(AWAIT_SECONDS, TimeUnit.SECONDS)).isEqualTo(driverId1);
        assertThat(future2.get(AWAIT_SECONDS, TimeUnit.SECONDS)).isEqualTo(driverId2);
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

        CompletableFuture<Long> future = new CompletableFuture<>();
        subscriber.registerFuture(tripId, future);
        subscriber.removeFuture(tripId);

        publisher.publish(tripId, "ACCEPT", 42L);

        Thread.sleep(NO_COMPLETE_WAIT_MS);
        assertThat(future).isNotDone();
    }

    @Test
    @DisplayName("Два ACCEPT подряд для одного tripId - future завершается первым driverId")
    void publish_doubleAccept_futureCompletedByFirstDriver() throws Exception {
        Long tripId    = 400L;
        Long driverId1 = 55L;
        Long driverId2 = 66L;

        CompletableFuture<Long> future = new CompletableFuture<>();
        subscriber.registerFuture(tripId, future);

        publisher.publish(tripId, "ACCEPT", driverId1);
        Long result = future.get(AWAIT_SECONDS, TimeUnit.SECONDS);

        assertThatCode(() -> publisher.publish(tripId, "ACCEPT", driverId2))
                .doesNotThrowAnyException();

        assertThat(result).isEqualTo(driverId1);
    }
}
package com.example.tripservice.unit.service;

import com.example.tripservice.dto.data.DriverResponseDto;
import com.example.tripservice.service.search.DriverResponseSubscriber;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;

class DriverResponseSubscriberTest {

    private DriverResponseSubscriber subscriber;

    @BeforeEach
    void setUp() {
        // Создаём вручную — без Spring, без Redis
        subscriber = new DriverResponseSubscriber();
    }

    // ─── onMessage: ACCEPT ────────────────────────────────────────────────────

    @Test
    @DisplayName("ACCEPT: future завершается с driverId")
    void onMessage_accept_completesFutureWithDriverId() throws Exception {
        CompletableFuture<DriverResponseDto> future = new CompletableFuture<>();
        subscriber.registerFuture(10L, future);

        subscriber.onMessage("ACCEPT:10:99");

        assertThat(future.get(1, TimeUnit.SECONDS).driverId()).isEqualTo(99L);
    }

    @Test
    @DisplayName("REJECT: future завершается с CancellationException")
    void onMessage_reject_completesFutureExceptionally() {
        CompletableFuture<DriverResponseDto> future = new CompletableFuture<>();
        subscriber.registerFuture(10L, future);

        subscriber.onMessage("REJECT:10:99");

        assertThat(future).isCompletedExceptionally();
        assertThatThrownBy(() -> future.get(1, TimeUnit.SECONDS))
                .isInstanceOf(CancellationException.class);
    }

    @Test
    @DisplayName("Неизвестный action: future остаётся незавершённой")
    void onMessage_unknownAction_futureNotCompleted() {
        CompletableFuture<DriverResponseDto> future = new CompletableFuture<>();
        subscriber.registerFuture(10L, future);

        subscriber.onMessage("UNKNOWN:10:42");

        assertThat(future).isNotDone();
    }

    @Test
    @DisplayName("Нет зарегистрированной future: сообщение игнорируется без исключений")
    void onMessage_noFutureRegistered_doesNotThrow() {
        assertThatCode(() -> subscriber.onMessage("ACCEPT:999:42"))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Невалидный формат сообщения: исключение поглощается внутри")
    void onMessage_malformedMessage_doesNotThrow() {
        assertThatCode(() -> subscriber.onMessage("totally_invalid"))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Пустое сообщение: исключение поглощается внутри")
    void onMessage_emptyMessage_doesNotThrow() {
        assertThatCode(() -> subscriber.onMessage(""))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Нечисловой driverId: исключение поглощается внутри")
    void onMessage_nonNumericDriverId_doesNotThrow() {
        CompletableFuture<Long> future = new CompletableFuture<>();
        subscriber.registerFuture(10L, future);

        assertThatCode(() -> subscriber.onMessage("ACCEPT:10:not-a-number"))
                .doesNotThrowAnyException();

        assertThat(future).isNotDone();
    }

    // ─── Изоляция между разными tripId ────────────────────────────────────────

    @Test
    @DisplayName("ACCEPT для tripId=1 не затрагивает future tripId=2")
    void onMessage_accept_onlyMatchingFutureCompleted() {
        CompletableFuture<Long> future1 = new CompletableFuture<>();
        CompletableFuture<Long> future2 = new CompletableFuture<>();
        subscriber.registerFuture(1L, future1);
        subscriber.registerFuture(2L, future2);

        subscriber.onMessage("ACCEPT:1:42");

        assertThat(future1).isCompletedWithValue(42L);
        assertThat(future2).isNotDone();
    }

    // ─── removeFuture ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("После removeFuture: ACCEPT не завершает удалённую future")
    void onMessage_afterRemoveFuture_futureNotCompleted() {
        CompletableFuture<Long> future = new CompletableFuture<>();
        subscriber.registerFuture(10L, future);
        subscriber.removeFuture(10L);

        subscriber.onMessage("ACCEPT:10:42");

        assertThat(future).isNotDone();
    }

    // ─── registerFuture: перезапись ───────────────────────────────────────────

    @Test
    @DisplayName("Перезапись future для того же tripId: завершается новая future")
    void registerFuture_overwrite_newFutureIsCompleted() throws Exception {
        CompletableFuture<Long> oldFuture = new CompletableFuture<>();
        CompletableFuture<Long> newFuture = new CompletableFuture<>();

        subscriber.registerFuture(10L, oldFuture);
        subscriber.registerFuture(10L, newFuture); // перезаписываем

        subscriber.onMessage("ACCEPT:10:55");

        assertThat(newFuture.get(1, TimeUnit.SECONDS)).isEqualTo(55L);
        assertThat(oldFuture).isNotDone(); // старая не завершена
    }

    // ─── Идемпотентность CompletableFuture ───────────────────────────────────

    @Test
    @DisplayName("Два ACCEPT подряд: второй вызов complete игнорируется")
    void onMessage_doubleAccept_secondCompleteIgnored() throws Exception {
        CompletableFuture<Long> future = new CompletableFuture<>();
        subscriber.registerFuture(10L, future);

        subscriber.onMessage("ACCEPT:10:42");
        // CF.complete() возвращает false при повторном вызове — не кидает исключение
        assertThatCode(() -> subscriber.onMessage("ACCEPT:10:99"))
                .doesNotThrowAnyException();

        assertThat(future.get(1, TimeUnit.SECONDS)).isEqualTo(42L); // первый победил
    }
}
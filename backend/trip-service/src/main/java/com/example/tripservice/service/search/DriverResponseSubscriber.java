package com.example.tripservice.service.search;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

// Класс хранит локальные pendingOffers (из-за CompletableFuture) и завершает их по Pub/Sub сигналу от redis
@Component
@RequiredArgsConstructor
@Slf4j
public class DriverResponseSubscriber {

    private final Map<Long, CompletableFuture<Long>> pendingOffers = new ConcurrentHashMap<>();

    public void registerFuture(Long tripId, CompletableFuture<Long> future) {
        pendingOffers.put(tripId, future);
    }

    public void removeFuture(Long tripId) {
        pendingOffers.remove(tripId);
    }

    // При получении сообщения из redis вызывается метод
    @SuppressWarnings("unused") // onMessage указан в RedisConfig
    public void onMessage(String message) {
        try {
            String[] parts = message.split(":");
            String action = parts[0]; // Статус ответа водителя ACCEPT или REJECT
            Long tripId = Long.parseLong(parts[1]);
            Long driverId = Long.parseLong(parts[2]);

            CompletableFuture<Long> future = pendingOffers.get(tripId);
            if (future == null) {
                log.debug("No pending future for trip {}, ignoring message", tripId);
                return;
            }

            switch (action) {
                case "ACCEPT" -> {
                    log.info("Driver {} accepted trip {}", driverId, tripId);
                    future.complete(driverId);
                }
                case "REJECT" -> {
                    log.info("Driver {} rejected trip {}", driverId, tripId);
                    future.completeExceptionally(new CancellationException());
                }
                default -> log.warn("Unknown driver response action: {}", action);
            }

        } catch (Exception e) {
            log.error("Failed to parse driver response message: '{}'", message, e);
        }
    }
}
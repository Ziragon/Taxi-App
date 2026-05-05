package com.example.tripservice.integration;

import com.example.shared.exception.common.AccessDeniedException;
import com.example.tripservice.BaseIntegrationTest;
import com.example.tripservice.service.cache.OfferCacheService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OfferCacheServiceIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private OfferCacheService offerCacheService;

    @BeforeEach
    void setUp() {
        clearKeysByPattern("active_offer:");
    }

    @Test
    @DisplayName("Установка оффера - должен возвращать сохраненного водителя")
    void setActiveOffer_shouldStoreAndRetrieveDriverId() {
        Long tripId = 1L;
        Long driverId = 500L;

        offerCacheService.setActiveOffer(tripId, driverId);
        Long cachedDriver = offerCacheService.getActiveOffer(tripId);

        assertThat(cachedDriver).isEqualTo(driverId);
    }

    @Test
    @DisplayName("Валидация - должен проходить успешно для правильного водителя")
    void validateActiveOffer_shouldPassForCorrectDriver() {
        Long tripId = 2L;
        Long driverId = 200L;

        offerCacheService.setActiveOffer(tripId, driverId);

        offerCacheService.validateAndRemoveActiveOffer(tripId, driverId);
    }

    @Test
    @DisplayName("Валидация - должен кидать AccessDenied для чужого водителя")
    void validateActiveOffer_shouldThrowAccessDenied_whenDriverMismatches() {
        Long tripId = 3L;
        Long actualDriverId = 300L;
        Long wrongDriverId = 999L;

        offerCacheService.setActiveOffer(tripId, actualDriverId);

        assertThrows(AccessDeniedException.class, () ->
                offerCacheService.validateAndRemoveActiveOffer(tripId, wrongDriverId)
        );
    }

    @Test
    @DisplayName("Удаление оффера - после удаления должен возвращать null")
    void removeActiveOffer_shouldDeleteKeyFromRedis() {
        Long tripId = 4L;
        offerCacheService.setActiveOffer(tripId, 500L);

        offerCacheService.removeActiveOffer(tripId);
        Long result = offerCacheService.getActiveOffer(tripId);

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("Валидация при пустом кэше - должен кидать AccessDenied")
    void validateActiveOffer_shouldThrowAccessDenied_whenOfferExpired() {
        Long tripId = 5L;
        Long driverId = 1000L;

        assertThrows(AccessDeniedException.class, () ->
                offerCacheService.validateAndRemoveActiveOffer(tripId, driverId)
        );
    }
}
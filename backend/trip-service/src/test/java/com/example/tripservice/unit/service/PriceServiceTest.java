package com.example.tripservice.unit.service;

import com.example.tripservice.dto.data.CalculatePriceDto;
import com.example.tripservice.dto.data.TariffPriceData;
import com.example.tripservice.service.PriceService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

class PriceServiceTest {

    private final PriceService priceService = new PriceService();

    @Test
    @DisplayName("Должен корректно рассчитать цену (выше minFare)")
    void calculatePrice_ShouldCalculateCorrectly_WhenAboveMinFare() {
        CalculatePriceDto dto = new CalculatePriceDto(
                new BigDecimal("50.0"), // baseFare
                new BigDecimal("100.0"),// minFare
                new BigDecimal("10.0"), // distanceKm
                new BigDecimal("20.0"), // durationMin
                new BigDecimal("15.0"), // pricePerKm
                new BigDecimal("5.0"), // pricePerMin
                new BigDecimal("1.1"), // weatherCoef
                new BigDecimal("1.2") // surgeCoef
        );
        // Distance: 10 * 15 = 150
        // Time: 20 * 5 = 100
        // Base: 50 + 150 + 100 = 300
        // Coefs: 300 * 1.1 * 1.2 = 396.00

        TariffPriceData result = priceService.calculatePrice(dto);

        assertThat(result.distanceCost()).isEqualByComparingTo("150.00");
        assertThat(result.timeCost()).isEqualByComparingTo("100.00");
        assertThat(result.price()).isEqualByComparingTo("396.00");
    }

    @Test
    @DisplayName("Должен применить minFare, если финальная цена < minFare")
    void calculatePrice_ShouldApplyMinFare_WhenTotalIsLower() {
        CalculatePriceDto dto = new CalculatePriceDto(
                new BigDecimal("10.0"), // base
                new BigDecimal("100.0"), // minFare
                new BigDecimal("1.0"),
                new BigDecimal("2.0"),
                new BigDecimal("10.0"),
                new BigDecimal("5.0"),
                new BigDecimal("1.0"), new BigDecimal("1.0")
        );
        // Total calculated = 30, but minFare is 100

        TariffPriceData result = priceService.calculatePrice(dto);

        assertThat(result.price()).isEqualByComparingTo("100.00");
    }

    @ParameterizedTest
    @CsvSource({
            "3, 1.2", // Ночь
            "8, 1.15", // Утренний час пик
            "18, 1.15", // Вечерний час пик
            "12, 1.0" // Обычное время
    })
    @DisplayName("Должен вернуть корректный surgeCoef в разное время дня")
    void getSurgeCoef_ShouldReturnCorrectMultiplier(int hour, String expectedCoef) {
        LocalDateTime time = LocalDateTime.of(2026, 4, 30, hour, 0);
        assertEquals(new BigDecimal(expectedCoef), priceService.getSurgeCoef(time));
    }
}
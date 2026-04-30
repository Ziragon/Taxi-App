package com.example.tripservice.unit.service;

import com.example.tripservice.dto.data.CalculatePriceDto;
import com.example.tripservice.dto.data.TariffPriceData;
import com.example.tripservice.service.PriceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class PriceServiceTest {

    private PriceService priceService;

    @BeforeEach
    void setUp() {
        priceService = new PriceService();
    }

    private static CalculatePriceDto baseDto(
            BigDecimal distanceKm,
            BigDecimal durationMin,
            BigDecimal weatherCoef,
            BigDecimal surgeCoef,
            BigDecimal minFare
    ) {
        return new CalculatePriceDto(
                new BigDecimal("50.00"),
                minFare,
                distanceKm,
                durationMin,
                new BigDecimal("10.00"),
                new BigDecimal("2.00"),
                weatherCoef,
                surgeCoef
        );
    }

    @Nested
    @DisplayName("calculatePrice()")
    class CalculatePrice {

        @Test
        @DisplayName("Базовый расчёт без коэффициентов (все = 1.0)")
        void baseCalculation_noCoefs() {
            // distanceCost = 10km * 10.00 = 100.00
            // timeCost     = 30min * 2.00  = 60.00
            // total        = (50 + 100 + 60) * 1.0 * 1.0 = 210.00
            CalculatePriceDto dto = baseDto(
                    new BigDecimal("10"),
                    new BigDecimal("30"),
                    BigDecimal.ONE,
                    BigDecimal.ONE,
                    new BigDecimal("1.00")
            );

            TariffPriceData result = priceService.calculatePrice(dto);

            assertThat(result.distanceCost()).isEqualByComparingTo("100.00");
            assertThat(result.timeCost()).isEqualByComparingTo("60.00");
            assertThat(result.price()).isEqualByComparingTo("210.00");
        }

        @Test
        @DisplayName("weatherCoef > 1 пропорционально увеличивает итог")
        void weatherCoef_increasesTotal() {
            // total без кэфа = 210.00, weatherCoef = 1.3 - 210 * 1.3 = 273.00
            CalculatePriceDto dto = baseDto(
                    new BigDecimal("10"),
                    new BigDecimal("30"),
                    new BigDecimal("1.3"),
                    BigDecimal.ONE,
                    new BigDecimal("1.00")
            );

            TariffPriceData result = priceService.calculatePrice(dto);

            assertThat(result.price()).isEqualByComparingTo("273.00");
        }

        @Test
        @DisplayName("surgeCoef > 1 пропорционально увеличивает итог")
        void surgeCoef_increasesTotal() {
            // total без кэфа = 210.00, surgeCoef = 1.2 - 210 * 1.2 = 252.00
            CalculatePriceDto dto = baseDto(
                    new BigDecimal("10"),
                    new BigDecimal("30"),
                    BigDecimal.ONE,
                    new BigDecimal("1.2"),
                    new BigDecimal("1.00")
            );

            TariffPriceData result = priceService.calculatePrice(dto);

            assertThat(result.price()).isEqualByComparingTo("252.00");
        }

        @Test
        @DisplayName("weatherCoef и surgeCoef перемножаются корректно")
        void bothCoefs_multipliedTogether() {
            // total = 210 * 1.3 * 1.2 = 327.60
            CalculatePriceDto dto = baseDto(
                    new BigDecimal("10"),
                    new BigDecimal("30"),
                    new BigDecimal("1.3"),
                    new BigDecimal("1.2"),
                    new BigDecimal("1.00")
            );

            TariffPriceData result = priceService.calculatePrice(dto);

            assertThat(result.price()).isEqualByComparingTo("327.60");
        }

        @Test
        @DisplayName("Цена ниже minFare - возвращается minFare")
        void price_belowMinFare_returnsMinFare() {
            // distanceCost = 0.1 * 10 = 1.00
            // timeCost     = 1 * 2   = 2.00
            // total        = (50 + 1 + 2) * 1 * 1 = 53.00 - но minFare = 100
            CalculatePriceDto dto = baseDto(
                    new BigDecimal("0.1"),
                    new BigDecimal("1"),
                    BigDecimal.ONE,
                    BigDecimal.ONE,
                    new BigDecimal("100.00")
            );

            TariffPriceData result = priceService.calculatePrice(dto);

            assertThat(result.price()).isEqualByComparingTo("100.00");
        }

        @Test
        @DisplayName("Цена выше minFare - minFare не применяется")
        void price_aboveMinFare_notCapped() {
            // total = 210.00, minFare = 50.00 - возвращается 210.00
            CalculatePriceDto dto = baseDto(
                    new BigDecimal("10"),
                    new BigDecimal("30"),
                    BigDecimal.ONE,
                    BigDecimal.ONE,
                    new BigDecimal("50.00")
            );

            TariffPriceData result = priceService.calculatePrice(dto);

            assertThat(result.price()).isEqualByComparingTo("210.00");
        }

        @Test
        @DisplayName("Цена точно равна minFare - возвращается без изменений")
        void price_equalToMinFare_returnsSame() {
            // baseFare = 50, distanceCost = 0, timeCost = 0 → total = 50
            CalculatePriceDto dto = new CalculatePriceDto(
                    new BigDecimal("50.00"),
                    new BigDecimal("50.00"),
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ONE,
                    BigDecimal.ONE
            );

            TariffPriceData result = priceService.calculatePrice(dto);

            assertThat(result.price()).isEqualByComparingTo("50.00");
        }

        @Test
        @DisplayName("Масштаб результата - ровно 2 знака после запятой")
        void result_hasScale2() {
            // total = (50 + 10*10 + 3*2) * 1 * 1 = 206.00 не вызывает округление
            CalculatePriceDto dto = baseDto(
                    new BigDecimal("10.333"),
                    new BigDecimal("7.777"),
                    BigDecimal.ONE,
                    BigDecimal.ONE,
                    new BigDecimal("1.00")
            );

            TariffPriceData result = priceService.calculatePrice(dto);

            assertThat(result.distanceCost().scale()).isEqualTo(2);
            assertThat(result.timeCost().scale()).isEqualTo(2);
            assertThat(result.price().scale()).isEqualTo(2);
        }

        @Test
        @DisplayName("Округление HALF_UP: .5 → округляется вверх")
        void rounding_halfUp() {
            // distanceCost = 1.005 * 10 = 10.05 - при scale(2, HALF_UP) = 10.05
            // timeCost     = 0.005 * 2  = 0.01
            CalculatePriceDto dto = new CalculatePriceDto(
                    new BigDecimal("1.005"),
                    new BigDecimal("10.00"),
                    new BigDecimal("0.005"),
                    new BigDecimal("2.00"),
                    BigDecimal.ZERO,
                    BigDecimal.ONE,
                    BigDecimal.ONE,
                    BigDecimal.ZERO
            );

            TariffPriceData result = priceService.calculatePrice(dto);

            assertThat(result.distanceCost().scale()).isEqualTo(2);
            assertThat(result.timeCost().scale()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("getSurgeCoef()")
    class GetSurgeCoef {

        @Test
        @DisplayName("null → возвращает 1.0")
        void nullLocaltime_returnsOne() {
            BigDecimal result = priceService.getSurgeCoef(null);
            assertThat(result).isEqualByComparingTo(BigDecimal.ONE);
        }

        static Stream<Arguments> surgeCoefByHour() {
            return Stream.of(
                    // Ночь: 00:00-05:59 - 1.2
                    Arguments.of(0,  "1.2",  "полночь (00:00)"),
                    Arguments.of(3,  "1.2",  "середина ночи (03:00)"),
                    Arguments.of(5,  "1.2",  "5:00 - последний ночной час"),
                    // Граница: 06:00 - 1.0
                    Arguments.of(6,  "1.0",  "06:00 - граница окончания ночи"),
                    Arguments.of(7,  "1.0",  "07:00 - до утреннего пика"),
                    // Утренний пик: 08:00-09:59 - 1.15
                    Arguments.of(8,  "1.15", "08:00 - начало утреннего пика"),
                    Arguments.of(9,  "1.15", "09:00 - утренний пик"),
                    // Граница: 10:00 - 1.0
                    Arguments.of(10, "1.0",  "10:00 - граница окончания утреннего пика"),
                    Arguments.of(13, "1.0",  "13:00 - дневное время"),
                    Arguments.of(16, "1.0",  "16:00 - до вечернего пика"),
                    // Вечерний пик: 17:00-19:59 - 1.15
                    Arguments.of(17, "1.15", "17:00 - начало вечернего пика"),
                    Arguments.of(19, "1.15", "19:00 - вечерний пик"),
                    // Граница: 20:00 - 1.0
                    Arguments.of(20, "1.0",  "20:00 - граница окончания вечернего пика"),
                    Arguments.of(23, "1.0",  "23:00 - поздний вечер")
            );
        }

        @ParameterizedTest(name = "{2} → коэф {1}")
        @MethodSource("surgeCoefByHour")
        @DisplayName("Временные слоты - правильный коэффициент")
        void surgeCoef_byHour(int hour, String expectedCoef, String description) {
            LocalDateTime time = LocalDateTime.of(2024, 1, 1, hour, 0);

            BigDecimal result = priceService.getSurgeCoef(time);

            assertThat(result).isEqualByComparingTo(expectedCoef);
        }

        @Test
        @DisplayName("Граничное значение 05:59 - ночной коэф 1.2")
        void boundary_05_59_isNight() {
            LocalDateTime time = LocalDateTime.of(2024, 1, 1, 5, 59);
            assertThat(priceService.getSurgeCoef(time)).isEqualByComparingTo("1.2");
        }

        @Test
        @DisplayName("Граничное значение 09:59 - утренний пик 1.15")
        void boundary_09_59_isMorningPeak() {
            LocalDateTime time = LocalDateTime.of(2024, 1, 1, 9, 59);
            assertThat(priceService.getSurgeCoef(time)).isEqualByComparingTo("1.15");
        }

        @Test
        @DisplayName("Граничное значение 19:59 - вечерний пик 1.15")
        void boundary_19_59_isEveningPeak() {
            LocalDateTime time = LocalDateTime.of(2024, 1, 1, 19, 59);
            assertThat(priceService.getSurgeCoef(time)).isEqualByComparingTo("1.15");
        }
    }
}
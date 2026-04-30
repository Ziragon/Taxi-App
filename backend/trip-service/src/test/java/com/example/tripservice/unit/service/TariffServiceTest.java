package com.example.tripservice.unit.service;

import com.example.shared.dto.enums.VehicleClass;
import com.example.tripservice.dto.data.CalculatePriceDto;
import com.example.tripservice.dto.data.TariffDto;
import com.example.tripservice.dto.data.TariffPriceData;
import com.example.tripservice.dto.data.TripDto;
import com.example.tripservice.entity.Tariff;
import com.example.tripservice.exception.TariffNotActiveException;
import com.example.tripservice.exception.TariffNotFoundException;
import com.example.tripservice.repository.TariffRepository;
import com.example.tripservice.service.PriceService;
import com.example.tripservice.service.TariffService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TariffServiceTest {

    @Mock
    private TariffRepository tariffRepository;

    @Mock
    private PriceService priceService;

    @InjectMocks
    private TariffService tariffService;

    private Tariff buildTariff(VehicleClass vehicleClass, boolean active) {
        return Tariff.builder()
                .id(1L)
                .tripClass(vehicleClass)
                .active(active)
                .baseFare(new BigDecimal("50.00"))
                .pricePerKm(new BigDecimal("10.00"))
                .pricePerMin(new BigDecimal("2.00"))
                .minFare(new BigDecimal("100.00"))
                .build();
    }

    private TripDto buildTripDto() {
        return mock(TripDto.class);
    }

    @Nested
    @DisplayName("getActiveTariffs()")
    class GetActiveTariffs {

        @Test
        @DisplayName("Репозиторий возвращает список - список пробрасывается без изменений")
        void returnsListFromRepo() {
            List<Tariff> tariffs = List.of(
                    buildTariff(VehicleClass.ECONOMY, true),
                    buildTariff(VehicleClass.COMFORT, true)
            );
            when(tariffRepository.findAllByActive(true)).thenReturn(tariffs);

            List<Tariff> result = tariffService.getActiveTariffs();

            assertThat(result).hasSize(2).containsExactlyElementsOf(tariffs);
        }

        @Test
        @DisplayName("Репозиторий возвращает пустой список - возвращается пустой список")
        void emptyList_returnedAsIs() {
            when(tariffRepository.findAllByActive(true)).thenReturn(List.of());

            List<Tariff> result = tariffService.getActiveTariffs();

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("В репозиторий всегда передаётся active=true")
        void repo_calledWithActiveTrue() {
            when(tariffRepository.findAllByActive(true)).thenReturn(List.of());

            tariffService.getActiveTariffs();

            verify(tariffRepository).findAllByActive(true);
            verify(tariffRepository, never()).findAllByActive(false);
        }
    }

    @Nested
    @DisplayName("getByVehicleClass()")
    class GetByVehicleClass {

        @Test
        @DisplayName("Тариф найден и активен - возвращается тариф")
        void found_active_returnsTariff() {
            Tariff tariff = buildTariff(VehicleClass.ECONOMY, true);
            when(tariffRepository.findByTripClass(VehicleClass.ECONOMY))
                    .thenReturn(Optional.of(tariff));

            Tariff result = tariffService.getByVehicleClass(VehicleClass.ECONOMY);

            assertThat(result).isEqualTo(tariff);
            assertThat(result.getTripClass()).isEqualTo(VehicleClass.ECONOMY);
        }

        @Test
        @DisplayName("Тариф не найден - TariffNotFoundException")
        void notFound_throwsTariffNotFoundException() {
            when(tariffRepository.findByTripClass(VehicleClass.BUSINESS))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> tariffService.getByVehicleClass(VehicleClass.BUSINESS))
                    .isInstanceOf(TariffNotFoundException.class);
        }

        @Test
        @DisplayName("Тариф найден, но active=false - TariffNotActiveException")
        void found_inactive_throwsTariffNotActiveException() {
            Tariff inactive = buildTariff(VehicleClass.COMFORT, false);
            when(tariffRepository.findByTripClass(VehicleClass.COMFORT))
                    .thenReturn(Optional.of(inactive));

            assertThatThrownBy(() -> tariffService.getByVehicleClass(VehicleClass.COMFORT))
                    .isInstanceOf(TariffNotActiveException.class);
        }

        @Test
        @DisplayName("TariffNotActiveException содержит vehicleClass")
        void inactive_exceptionContainsVehicleClass() {
            Tariff inactive = buildTariff(VehicleClass.COMFORT, false);
            when(tariffRepository.findByTripClass(VehicleClass.COMFORT))
                    .thenReturn(Optional.of(inactive));

            assertThatThrownBy(() -> tariffService.getByVehicleClass(VehicleClass.COMFORT))
                    .isInstanceOf(TariffNotActiveException.class)
                    .hasMessageContaining("COMFORT");
        }

        @Test
        @DisplayName("TariffNotFoundException содержит vehicleClass")
        void notFound_exceptionContainsVehicleClass() {
            when(tariffRepository.findByTripClass(VehicleClass.BUSINESS))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> tariffService.getByVehicleClass(VehicleClass.BUSINESS))
                    .isInstanceOf(TariffNotFoundException.class)
                    .hasMessageContaining("BUSINESS");
        }

        @Test
        @DisplayName("Репозиторий вызывается с нужным vehicleClass")
        void repo_calledWithCorrectClass() {
            when(tariffRepository.findByTripClass(VehicleClass.ECONOMY))
                    .thenReturn(Optional.of(buildTariff(VehicleClass.ECONOMY, true)));

            tariffService.getByVehicleClass(VehicleClass.ECONOMY);

            verify(tariffRepository).findByTripClass(VehicleClass.ECONOMY);
        }
    }

    @Nested
    @DisplayName("calculatePrice()")
    class CalculatePrice {

        @Test
        @DisplayName("Делегирует в PriceService с корректным CalculatePriceDto")
        void delegatesToPriceService() {
            Tariff tariff = buildTariff(VehicleClass.ECONOMY, true);
            TripDto tripDto = buildTripDto();
            TariffPriceData priceData = new TariffPriceData(
                    new BigDecimal("100.00"),
                    new BigDecimal("60.00"),
                    new BigDecimal("210.00")
            );
            when(priceService.calculatePrice(any(CalculatePriceDto.class))).thenReturn(priceData);

            tariffService.calculatePrice(tariff, tripDto);

            verify(priceService, times(1)).calculatePrice(any(CalculatePriceDto.class));
        }

        @Test
        @DisplayName("CalculatePriceDto строится из полей тарифа корректно")
        void calculatePriceDto_builtFromTariff() {
            Tariff tariff = buildTariff(VehicleClass.ECONOMY, true);
            TripDto tripDto = buildTripDto();
            ArgumentCaptor<CalculatePriceDto> captor = ArgumentCaptor.forClass(CalculatePriceDto.class);
            TariffPriceData priceData = new TariffPriceData(
                    BigDecimal.TEN, BigDecimal.TEN, new BigDecimal("210.00")
            );
            when(priceService.calculatePrice(captor.capture())).thenReturn(priceData);

            tariffService.calculatePrice(tariff, tripDto);

            CalculatePriceDto captured = captor.getValue();
            assertThat(captured.baseFare()).isEqualByComparingTo(tariff.getBaseFare());
            assertThat(captured.pricePerKm()).isEqualByComparingTo(tariff.getPricePerKm());
            assertThat(captured.pricePerMin()).isEqualByComparingTo(tariff.getPricePerMin());
            assertThat(captured.minFare()).isEqualByComparingTo(tariff.getMinFare());
        }

        @Test
        @DisplayName("Результат TariffDto содержит prices из PriceService")
        void result_containsPricesFromPriceService() {
            Tariff tariff = buildTariff(VehicleClass.ECONOMY, true);
            TripDto tripDto = buildTripDto();
            TariffPriceData priceData = new TariffPriceData(
                    new BigDecimal("100.00"),
                    new BigDecimal("60.00"),
                    new BigDecimal("210.00")
            );
            when(priceService.calculatePrice(any())).thenReturn(priceData);

            TariffDto result = tariffService.calculatePrice(tariff, tripDto);

            assertThat(result.prices()).isEqualTo(priceData);
        }

        @Test
        @DisplayName("Результат TariffDto содержит данные тарифа - vehicleClass, ставки")
        void result_containsTariffFields() {
            Tariff tariff = buildTariff(VehicleClass.COMFORT, true);
            TripDto tripDto = buildTripDto();
            when(priceService.calculatePrice(any())).thenReturn(
                    new TariffPriceData(BigDecimal.TEN, BigDecimal.TEN, new BigDecimal("210.00"))
            );

            TariffDto result = tariffService.calculatePrice(tariff, tripDto);

            assertThat(result.tripClass()).isEqualTo(VehicleClass.COMFORT);
            assertThat(result.baseFare()).isEqualByComparingTo(tariff.getBaseFare());
            assertThat(result.pricePerKm()).isEqualByComparingTo(tariff.getPricePerKm());
            assertThat(result.pricePerMin()).isEqualByComparingTo(tariff.getPricePerMin());
        }

        @Test
        @DisplayName("driverCount в TariffDto = null, когда передан null")
        void result_driverCountIsNull_whenPassedNull() {
            Tariff tariff = buildTariff(VehicleClass.ECONOMY, true);
            when(priceService.calculatePrice(any())).thenReturn(
                    new TariffPriceData(BigDecimal.TEN, BigDecimal.TEN, new BigDecimal("100.00"))
            );

            TariffDto result = tariffService.calculatePrice(tariff, buildTripDto());

            assertThat(result.driversNearby()).isNull();
        }
    }
}
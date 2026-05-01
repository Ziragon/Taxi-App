package com.example.tripservice.integration;

import com.example.shared.dto.enums.VehicleClass;
import com.example.tripservice.config.TestContainersConfig;
import com.example.tripservice.entity.Tariff;
import com.example.tripservice.exception.TariffNotActiveException;
import com.example.tripservice.exception.TariffNotFoundException;
import com.example.tripservice.repository.TariffRepository;
import com.example.tripservice.service.TariffService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Import(TestContainersConfig.class)
@ActiveProfiles("test")
@Transactional
public class TariffServiceIntegrationTest {

    @Autowired
    private TariffService tariffService;

    @Autowired
    private TariffRepository tariffRepository;

    @Autowired
    private EntityManager em;

    private Tariff persistTariff(VehicleClass vehicleClass, boolean active) {
        Tariff tariff = Tariff.builder()
                .tripClass(vehicleClass)
                .active(active)
                .baseFare(new BigDecimal("50.00"))
                .pricePerKm(new BigDecimal("10.00"))
                .pricePerMin(new BigDecimal("2.00"))
                .minFare(new BigDecimal("100.00"))
                .build();
        tariffRepository.save(tariff);
        em.flush();
        em.clear();
        return tariff;
    }

    @Nested
    @DisplayName("getActiveTariffs() - реальная БД")
    class GetActiveTariffs {

        @Test
        @DisplayName("2 активных + 1 неактивный - возвращаются только 2 активных")
        void returnsOnlyActiveTariffs() {
            persistTariff(VehicleClass.ECONOMY, true);
            persistTariff(VehicleClass.COMFORT, true);
            persistTariff(VehicleClass.BUSINESS, false);

            List<Tariff> result = tariffService.getActiveTariffs();

            assertThat(result).hasSize(2);
            assertThat(result).allMatch(Tariff::isActive);
        }

        @Test
        @DisplayName("Все тарифы неактивны - пустой список")
        void allInactive_returnsEmpty() {
            persistTariff(VehicleClass.ECONOMY, false);
            persistTariff(VehicleClass.COMFORT, false);

            List<Tariff> result = tariffService.getActiveTariffs();

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Таблица пустая - пустой список")
        void emptyTable_returnsEmpty() {
            List<Tariff> result = tariffService.getActiveTariffs();

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Возвращаются тарифы только с корректными vehicleClass")
        void returnedTariffs_haveCorrectVehicleClasses() {
            persistTariff(VehicleClass.ECONOMY, true);
            persistTariff(VehicleClass.COMFORT, true);

            List<Tariff> result = tariffService.getActiveTariffs();

            assertThat(result)
                    .extracting(Tariff::getTripClass)
                    .containsExactlyInAnyOrder(VehicleClass.ECONOMY, VehicleClass.COMFORT);
        }
    }

    @Nested
    @DisplayName("getByVehicleClass() - реальная БД")
    class GetByVehicleClass {

        @Test
        @DisplayName("Тариф с нужным классом есть и активен - возвращается корректный тариф")
        void found_active_returnsCorrectTariff() {
            Tariff saved = persistTariff(VehicleClass.ECONOMY, true);

            Tariff result = tariffService.getByVehicleClass(VehicleClass.ECONOMY);

            assertThat(result.getId()).isEqualTo(saved.getId());
            assertThat(result.getTripClass()).isEqualTo(VehicleClass.ECONOMY);
            assertThat(result.isActive()).isTrue();
        }

        @Test
        @DisplayName("Тарифа с данным классом нет - TariffNotFoundException")
        void notFound_throwsTariffNotFoundException() {
            persistTariff(VehicleClass.ECONOMY, true);

            assertThatThrownBy(() -> tariffService.getByVehicleClass(VehicleClass.BUSINESS))
                    .isInstanceOf(TariffNotFoundException.class);
        }

        @Test
        @DisplayName("Тариф есть, но inactive - TariffNotActiveException")
        void found_inactive_throwsTariffNotActiveException() {
            persistTariff(VehicleClass.COMFORT, false);

            assertThatThrownBy(() -> tariffService.getByVehicleClass(VehicleClass.COMFORT))
                    .isInstanceOf(TariffNotActiveException.class);
        }

        @Test
        @DisplayName("Несколько тарифов в БД - возвращается только нужный")
        void multipleTariffs_returnsCorrectOne() {
            persistTariff(VehicleClass.ECONOMY, true);
            persistTariff(VehicleClass.COMFORT, true);
            persistTariff(VehicleClass.BUSINESS, true);

            Tariff result = tariffService.getByVehicleClass(VehicleClass.COMFORT);

            assertThat(result.getTripClass()).isEqualTo(VehicleClass.COMFORT);
        }

        @Test
        @DisplayName("Финансовые поля тарифа корректно читаются из БД")
        void financialFields_readCorrectlyFromDb() {
            persistTariff(VehicleClass.ECONOMY, true);

            Tariff result = tariffService.getByVehicleClass(VehicleClass.ECONOMY);

            assertThat(result.getBaseFare()).isEqualByComparingTo("50.00");
            assertThat(result.getPricePerKm()).isEqualByComparingTo("10.00");
            assertThat(result.getPricePerMin()).isEqualByComparingTo("2.00");
            assertThat(result.getMinFare()).isEqualByComparingTo("100.00");
        }
    }
}
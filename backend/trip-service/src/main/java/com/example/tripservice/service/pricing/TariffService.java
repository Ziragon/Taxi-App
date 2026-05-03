package com.example.tripservice.service.pricing;

import com.example.shared.dto.enums.VehicleClass;
import com.example.tripservice.dto.data.CalculatePriceDto;
import com.example.tripservice.dto.data.TariffDto;
import com.example.tripservice.dto.data.TariffPriceData;
import com.example.tripservice.dto.data.TripDto;
import com.example.tripservice.entity.Tariff;
import com.example.tripservice.exception.TariffNotActiveException;
import com.example.tripservice.exception.TariffNotFoundException;
import com.example.tripservice.repository.TariffRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TariffService {

    private final TariffRepository tariffRepository;
    private final PriceService priceService;

    @Transactional(readOnly = true)
    public List<Tariff> getActiveTariffs() {
        return tariffRepository.findAllByActive(true);
    }

    @Transactional(readOnly = true)
    public Tariff getByVehicleClass(VehicleClass vehicleClass) {

        Tariff tariff = tariffRepository.findByTripClass(vehicleClass)
                .orElseThrow(() -> new TariffNotFoundException(vehicleClass));

        if (!tariff.isActive()) {
            throw new TariffNotActiveException(vehicleClass);
        }

        return tariff;
    }

    public TariffDto calculatePrice(Tariff tariff, TripDto tripDto) {

        TariffPriceData prices = priceService.calculatePrice(
                CalculatePriceDto.from(tariff, tripDto)
        );

        return TariffDto.from(tariff, prices, null);
    }
}

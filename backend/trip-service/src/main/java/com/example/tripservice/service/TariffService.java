package com.example.tripservice.service;

import com.example.tripservice.dto.data.TariffDto;
import com.example.tripservice.dto.data.TariffPriceData;
import com.example.tripservice.dto.data.TripDto;
import com.example.tripservice.entity.Tariff;
import com.example.tripservice.repository.TariffRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TariffService {

    private final TariffRepository tariffRepository;
    private final PriceService priceService;

    public List<TariffDto> calculateAllTariffs(TripDto tripDto) {

        List<Tariff> tariffs = tariffRepository.findAllByActive(true);

        return tariffs.stream()
                .map(tariff -> {
                    TariffPriceData calculatedPrices = priceService.calculatePrice(
                            tariff.getBaseFare(),
                            tripDto.distanceKm(),
                            tripDto.durationMin(),
                            tariff.getPricePerKm(),
                            tariff.getPricePerMin(),
                            tripDto.weatherCoef(),
                            tripDto.surgeCoef()
                    );

                    return TariffDto.from(tariff, calculatedPrices);
                })
                .toList();
    }
}

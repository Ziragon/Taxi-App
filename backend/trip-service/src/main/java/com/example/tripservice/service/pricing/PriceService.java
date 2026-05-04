package com.example.tripservice.service.pricing;

import com.example.tripservice.dto.data.CalculatePriceDto;
import com.example.tripservice.dto.data.TariffPriceData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class PriceService {

    // Рассчитывание окончательной цены по тарифу со всеми параметрами
    public TariffPriceData calculatePrice(CalculatePriceDto dto) {

        BigDecimal distanceCost = dto.distanceKm().multiply(dto.pricePerKm());

        BigDecimal timeCost = dto.durationMin().multiply(dto.pricePerMin());

        BigDecimal totalPrice = dto.baseFare()
                .add(distanceCost)
                .add(timeCost)
                .multiply(dto.weatherCoef())
                .multiply(dto.surgeCoef());

        totalPrice = totalPrice.max(dto.minFare()); // если цена получилась меньше minFare - берем minFare

        return new TariffPriceData(
                distanceCost.setScale(2, RoundingMode.HALF_UP),
                timeCost.setScale(2, RoundingMode.HALF_UP),
                totalPrice.setScale(2, RoundingMode.HALF_UP)
        );
    }

    // Имитация пробок
    public BigDecimal getSurgeCoef(LocalDateTime localtime) {

        if (localtime == null) return BigDecimal.ONE;

        int hour = localtime.getHour();

        if (hour < 6) { // От 12 ночи до 6
            return new BigDecimal("1.2");
        } else if(
                (hour >= 8 && hour < 10) ||
                (hour >= 17 && hour < 20)
        ) {
            return new BigDecimal("1.15");
        }

        return new BigDecimal("1.0");
    }
}

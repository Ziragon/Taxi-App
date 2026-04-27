package com.example.tripservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class PriceService {

    // Имитация пробок
    public BigDecimal getSurgeCoef(LocalDateTime localtime) {

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

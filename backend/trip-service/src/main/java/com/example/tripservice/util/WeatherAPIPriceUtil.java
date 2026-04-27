package com.example.tripservice.util;

import lombok.experimental.UtilityClass;

import java.math.BigDecimal;
import java.util.Set;

@UtilityClass
public class WeatherAPIPriceUtil {

    private static final Set<Integer> NORMAL_CODES = Set.of(1000, 1003, 1006, 1009);

    private static final Set<Integer> SEVERE_CODES = Set.of(
            1114, 1117, // Метель и снег с ветром
            1192, 1195, // Сильный дождь
            1201, 1207, // Ледяной дождь и сильный град
            1222, 1225, // Сильный снегопад
            1237,       // Ледяная крупа
            1243, 1246, // Сильные ливни
            1252, 1258, // Сильный снег/дождь со снегом
            1261, 1264, // Град
            1273, 1276, 1279, 1282 // Все виды гроз
    );

    public static BigDecimal getMultiplier(Integer code) {
        if (code == null || NORMAL_CODES.contains(code)) {
            return new BigDecimal("1.0");
        }
        if (SEVERE_CODES.contains(code)) {
            return new BigDecimal("1.2");
        }
        // Все остальные промежуточные условия (дождь, туман и тд)
        return new BigDecimal("1.1");
    }
}
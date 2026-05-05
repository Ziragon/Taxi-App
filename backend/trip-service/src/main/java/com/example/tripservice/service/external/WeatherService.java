package com.example.tripservice.service.external;

import com.example.tripservice.client.WeatherAPIClient;
import com.example.tripservice.dto.data.WeatherDto;
import com.example.tripservice.dto.response.WeatherResponse;
import com.example.tripservice.util.WeatherAPIPriceUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class WeatherService {

    private final WeatherAPIClient weatherClient;

    @Value("${weather-api.key}")
    private String apiKey;

    // Кэширование погоды в Redis по радиусу прим. 11 км.
    // Пример: координаты 55.0345, 82.9234 запишутся как 550_829 (точность около 11 км)
    @Cacheable(value = "weather", key =
                    "T(java.lang.Math).round((#latitude * 10).doubleValue()) + '_' + " +
                    "T(java.lang.Math).round((#longitude * 10).doubleValue())",
            unless = "#result == null || #result.location == null")
    public WeatherDto getWeatherCoef(BigDecimal longitude, BigDecimal latitude) {
        try {
            String currentCoords = String.format(java.util.Locale.US, "%f,%f", latitude, longitude);

            WeatherResponse response = weatherClient.getWeather(
                    apiKey,
                    currentCoords,
                    "ru"
            );

            BigDecimal weatherCoef = WeatherAPIPriceUtil.getMultiplier(response.current().condition().code());

            return WeatherDto.from(response, weatherCoef);
        } catch (Exception e) {
            log.warn("Weather API Error: {}", e.getMessage());
            return new WeatherDto(null, null, 0.0, "Weather Service Unavailable", null, new BigDecimal("1.0"));
        }
    }
}

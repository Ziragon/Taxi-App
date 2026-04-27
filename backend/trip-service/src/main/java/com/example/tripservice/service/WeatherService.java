package com.example.tripservice.service;

import com.example.tripservice.client.WeatherAPIClient;
import com.example.tripservice.dto.data.WeatherDto;
import com.example.tripservice.dto.response.WeatherResponse;
import com.example.tripservice.util.WeatherAPIPriceUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class WeatherService {

    private final WeatherAPIClient weatherClient;

    @Value("${weather-api.key}")
    private String apiKey;

    public WeatherDto getWeatherCoef(BigDecimal longitude, BigDecimal latitude) {
        try {
            String currentCoords = String.format("%f,%f", latitude, longitude);

            WeatherResponse response = weatherClient.getWeather(
                    apiKey,
                    currentCoords,
                    "ru"
            );

            BigDecimal weatherCoef = WeatherAPIPriceUtil.getMultiplier(response.current().condition().code());

            log.info(response.toString());

            return WeatherDto.from(response, weatherCoef);
        } catch (Exception e) {
            log.warn("Weather API Error: {}", e.getMessage());
            return new WeatherDto(null, 0.0, "Weather Service Unavailable", null, new BigDecimal("1.0"));
        }
    }
}

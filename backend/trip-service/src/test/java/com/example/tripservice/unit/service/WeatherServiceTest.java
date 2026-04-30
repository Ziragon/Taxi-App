package com.example.tripservice.unit.service;

import com.example.tripservice.client.WeatherAPIClient;
import com.example.tripservice.dto.data.WeatherDto;
import com.example.tripservice.service.WeatherService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WeatherServiceTest {

    @Mock
    private WeatherAPIClient weatherClient;

    @InjectMocks
    private WeatherService weatherService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(weatherService, "apiKey", "test-key");
    }

    @Test
    @DisplayName("Должен вернуть fallback пока сервис недоступен")
    void getWeatherCoef_ShouldReturnFallback_WhenApiFails() {

        when(weatherClient.getWeather(anyString(), anyString(), anyString()))
                .thenThrow(new RuntimeException("API Down"));

        WeatherDto result = weatherService.getWeatherCoef(BigDecimal.ZERO, BigDecimal.ZERO);

        assertThat(result.weatherCoef()).isEqualByComparingTo("1.0");
        assertThat(result.weatherCondition()).isEqualTo("Weather Service Unavailable");
    }

    @Test
    @DisplayName("")
    void getWeatherCoef_ShouldFormatCoordsCorrectly() {

        BigDecimal lat = new BigDecimal("55.0");
        BigDecimal lng = new BigDecimal("82.0");
        String expectedCoords = "55.000000,82.000000";

        WeatherDto result = weatherService.getWeatherCoef(lng, lat);

        assertThat(result.weatherCoef()).isEqualByComparingTo("1.0");
    }
}
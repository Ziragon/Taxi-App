package com.example.tripservice.integration;

import com.example.tripservice.BaseIntegrationTest;
import com.example.tripservice.dto.data.WeatherDto;
import com.example.tripservice.dto.response.WeatherResponse;
import com.example.tripservice.service.external.WeatherService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class WeatherServiceIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private WeatherService weatherService;

    @BeforeEach
    void setUp() {
        clearCache("weather");
    }

    private WeatherResponse buildMockResponse(String locationName, double temp, int code, String conditionText) {
        WeatherResponse.Condition condition = new WeatherResponse.Condition(conditionText, code);
        WeatherResponse.Current    current  = new WeatherResponse.Current(temp, condition);
        WeatherResponse.Location   location = new WeatherResponse.Location(locationName, LocalDateTime.now());
        return new WeatherResponse(location, current);
    }

    @Test
    @DisplayName("Разные вызовы - вызов клиента дважды")
    void getWeatherCoef_differentCoords_clientInvokedTwice() {
        when(weatherClient.getWeather(anyString(), anyString(), anyString()))
                .thenReturn(buildMockResponse("Moscow",           20.0, 1000, "Sunny"))
                .thenReturn(buildMockResponse("Saint Petersburg", 12.0, 1063, "Overcast"));

        weatherService.getWeatherCoef(new BigDecimal("30.92"), new BigDecimal("30.03"));
        weatherService.getWeatherCoef(new BigDecimal("52.32"), new BigDecimal("30.93"));

        verify(weatherClient, times(2)).getWeather(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("Exception клиента кидает fallback")
    void getWeatherCoef_clientThrowsException_returnsFallbackDto() {
        when(weatherClient.getWeather(anyString(), anyString(), anyString()))
                .thenThrow(new RuntimeException("API unavailable"));

        WeatherDto result = weatherService.getWeatherCoef(
                new BigDecimal("0.0"), new BigDecimal("0.0")
        );

        assertThat(result).isNotNull();
        assertThat(result.weatherCondition()).isEqualTo("Weather Service Unavailable");
        assertThat(result.weatherCoef()).isEqualByComparingTo(new BigDecimal("1.0"));
        assertThat(result.location()).isNull();
    }
}

package com.example.tripservice.integration;

import com.example.tripservice.client.WeatherAPIClient;
import com.example.tripservice.config.TestContainersConfig;
import com.example.tripservice.dto.data.WeatherDto;
import com.example.tripservice.dto.response.WeatherResponse;
import com.example.tripservice.service.WeatherService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@SpringBootTest
@Import(TestContainersConfig.class)
@ActiveProfiles("test")
class WeatherServiceIntegrationTest {

    @Autowired
    private WeatherService weatherService;

    @Autowired
    private CacheManager cacheManager;

    @MockitoBean
    private WeatherAPIClient weatherClient;

    @BeforeEach
    void clearCaches() {
        cacheManager.getCacheNames()
                .forEach(name -> Objects.requireNonNull(cacheManager.getCache(name)).clear());
    }

    private WeatherResponse buildMockResponse(String locationName, double temp, int code, String conditionText) {
        WeatherResponse.Condition condition = new WeatherResponse.Condition(conditionText, code);
        WeatherResponse.Current    current  = new WeatherResponse.Current(temp, condition);
        WeatherResponse.Location   location = new WeatherResponse.Location(locationName, LocalDateTime.now());
        return new WeatherResponse(location, current);
    }

    @Test
    @DisplayName("Идентичные вызовы - кэш-хит")
    void getWeatherCoef_sameCoordsCalledTwice_clientInvokedOnce() {
        WeatherResponse response = buildMockResponse("Moscow", 20.0, 1000, "Sunny");
        when(weatherClient.getWeather(anyString(), anyString(), anyString())).thenReturn(response);

        BigDecimal lat = new BigDecimal("55.7500");
        BigDecimal lng = new BigDecimal("37.6200");

        WeatherDto first  = weatherService.getWeatherCoef(lng, lat);
        WeatherDto second = weatherService.getWeatherCoef(lng, lat);

        assertThat(first).isEqualTo(second);
        verify(weatherClient, times(1)).getWeather(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("Разные вызовы - вызов клиента дважды")
    void getWeatherCoef_differentCoords_clientInvokedTwice() {
        when(weatherClient.getWeather(anyString(), anyString(), anyString()))
                .thenReturn(buildMockResponse("Moscow",           20.0, 1000, "Sunny"))
                .thenReturn(buildMockResponse("Saint Petersburg", 12.0, 1063, "Overcast"));

        weatherService.getWeatherCoef(new BigDecimal("82.92"), new BigDecimal("55.03"));
        weatherService.getWeatherCoef(new BigDecimal("30.32"), new BigDecimal("59.93"));

        verify(weatherClient, times(2)).getWeather(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("Округленное значение - кэш-хит")
    void getWeatherCoef_closeCoordsWithinApprox11km_hitsSameCacheEntry() {
        WeatherResponse response = buildMockResponse("Novosibirsk", 5.0, 1000, "Clear");
        when(weatherClient.getWeather(anyString(), anyString(), anyString())).thenReturn(response);

        // lat*10: round(550.3)=550, round(550.4)=550 одинаково
        // lng*10: round(829.2)=829, round(829.3)=829 одинаково
        BigDecimal lat1 = new BigDecimal("55.03"), lng1 = new BigDecimal("82.92");
        BigDecimal lat2 = new BigDecimal("55.04"), lng2 = new BigDecimal("82.93");

        weatherService.getWeatherCoef(lng1, lat1);
        weatherService.getWeatherCoef(lng2, lat2);

        verify(weatherClient, times(1)).getWeather(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("Exception клиента кидает fallback")
    void getWeatherCoef_clientThrowsException_returnsFallbackDto() {
        when(weatherClient.getWeather(anyString(), anyString(), anyString()))
                .thenThrow(new RuntimeException("API unavailable"));

        WeatherDto result = weatherService.getWeatherCoef(
                new BigDecimal("37.62"), new BigDecimal("55.75")
        );

        assertThat(result).isNotNull();
        assertThat(result.weatherCondition()).isEqualTo("Weather Service Unavailable");
        assertThat(result.weatherCoef()).isEqualByComparingTo(new BigDecimal("1.0"));
        assertThat(result.location()).isNull();
    }
}

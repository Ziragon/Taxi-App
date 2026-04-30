package com.example.tripservice.unit.service;

import com.example.shared.exception.common.ServiceUnavailableException;
import com.example.tripservice.client.OsrmClient;
import com.example.tripservice.dto.data.RouteDto;
import com.example.tripservice.dto.response.OsrmResponse;
import com.example.tripservice.exception.RouteNotFoundException;
import com.example.tripservice.service.NavigationService;
import feign.FeignException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NavigationServiceTest {

    @Mock
    private OsrmClient osrmClient;

    @InjectMocks
    private NavigationService navigationService;

    @Test
    @DisplayName("Выкидывает NotFound если путь не найден")
    void getRouteInfo_ShouldThrowRouteNotFound_WhenApiCodeIsNotOk() {
        OsrmResponse badResponse = new OsrmResponse("NoRoute", Collections.emptyList());
        when(osrmClient.getRoute(anyString(), anyString())).thenReturn(badResponse);

        assertThrows(RouteNotFoundException.class, () ->
                navigationService.getRouteInfo(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ONE, BigDecimal.ONE)
        );
    }

    @Test
    @DisplayName("Выкидывает ServiceUnavailable если сервис недоступен")
    void getRouteInfo_ShouldThrowServiceUnavailable_WhenFeignExceptionOccurs() {
        when(osrmClient.getRoute(anyString(), anyString()))
                .thenThrow(mock(FeignException.class));

        assertThrows(ServiceUnavailableException.class, () ->
                navigationService.getRouteInfo(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ONE, BigDecimal.ONE)
        );
    }

    @Test
    @DisplayName("Возвращает нужный маршрут по запросу")
    void getRouteInfo_ShouldReturnFirstRoute_OnSuccess() {
        RouteDto expectedRoute = new RouteDto(10.5, 600, null);
        OsrmResponse successResponse = new OsrmResponse("Ok", List.of(expectedRoute));

        when(osrmClient.getRoute(anyString(), anyString())).thenReturn(successResponse);

        RouteDto result = navigationService.getRouteInfo(
                new BigDecimal("82.0"), new BigDecimal("55.0"),
                new BigDecimal("82.1"), new BigDecimal("55.1")
        );

        org.junit.jupiter.api.Assertions.assertEquals(expectedRoute, result);
    }
}
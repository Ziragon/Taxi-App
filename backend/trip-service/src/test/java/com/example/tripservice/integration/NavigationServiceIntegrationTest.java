package com.example.tripservice.integration;

import com.example.shared.exception.common.ServiceUnavailableException;
import com.example.tripservice.BaseIntegrationTest;
import com.example.tripservice.dto.data.RouteDto;
import com.example.tripservice.dto.response.OsrmResponse;
import com.example.tripservice.exception.RouteNotFoundException;
import com.example.tripservice.service.NavigationService;
import feign.FeignException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class NavigationServiceIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private NavigationService navigationService;

    @BeforeEach
    void setUp() {
        clearCache("routes");
    }

    private OsrmResponse buildSuccessResponse(double distance, int duration, String geometry) {
        RouteDto route = new RouteDto(distance, duration, geometry);
        return new OsrmResponse("Ok", List.of(route));
    }

    private OsrmResponse buildFailResponse() {
        return new OsrmResponse("NoRoute", List.of());
    }

    @Test
    @DisplayName("Разные вызовы - вызов клиента дважды")
    void getRouteInfo_differentCoords_clientInvokedTwice() {
        when(osrmClient.getRoute(anyString(), anyString()))
                .thenReturn(buildSuccessResponse(5_000.0, 600,  "polyline_A"))
                .thenReturn(buildSuccessResponse(12_000.0, 900, "polyline_B"));

        RouteDto first = navigationService.getRouteInfo(
                new BigDecimal("98.98"), new BigDecimal("98.98"),
                new BigDecimal("98.98"), new BigDecimal("98.98")
        );
        RouteDto second = navigationService.getRouteInfo(
                new BigDecimal("89.89"), new BigDecimal("89.89"),
                new BigDecimal("89.89"), new BigDecimal("89.89")
        );

        assertThat(first.geometry()).isNotEqualTo(second.geometry());
        verify(osrmClient, times(2)).getRoute(anyString(), anyString());
    }

    @Test
    @DisplayName("OSRM выдал ошибку - RouteNotFoundException")
    void getRouteInfo_osrmNonOkCode_throwsRouteNotFoundException() {
        OsrmResponse failResponse = buildFailResponse();
        when(osrmClient.getRoute(anyString(), anyString())).thenReturn(failResponse);

        assertThatThrownBy(() ->
                navigationService.getRouteInfo(
                        new BigDecimal("67.67"), new BigDecimal("67.67"),
                        new BigDecimal("67.67"), new BigDecimal("67.67")
                )
        ).isInstanceOf(RouteNotFoundException.class);
    }

    @Test
    @DisplayName("FeignException - ServiceUnavailableException")
    void getRouteInfo_feignException_throwsServiceUnavailableException() {
        when(osrmClient.getRoute(anyString(), anyString()))
                .thenThrow(mock(FeignException.class));

        assertThatThrownBy(() ->
                navigationService.getRouteInfo(
                        new BigDecimal("99.99"), new BigDecimal("99.99"),
                        new BigDecimal("99.99"), new BigDecimal("99.99")
                )
        ).isInstanceOf(ServiceUnavailableException.class);
    }

    @Test
    @DisplayName("Корректная выдача результата после Exception")
    void getRouteInfo_afterFailure_successfulRetryGoesToClient() {
        when(osrmClient.getRoute(anyString(), anyString()))
                .thenReturn(buildFailResponse())
                .thenReturn(buildSuccessResponse(5_000.0, 600, "polyline_ok"));

        assertThatThrownBy(() ->
                navigationService.getRouteInfo(
                        new BigDecimal("11.11"), new BigDecimal("11.11"),
                        new BigDecimal("11.11"), new BigDecimal("11.11")
                )
        ).isInstanceOf(RouteNotFoundException.class);

        RouteDto result = navigationService.getRouteInfo(
                new BigDecimal("11.11"), new BigDecimal("11.11"),
                new BigDecimal("11.11"), new BigDecimal("11.11")
        );

        assertThat(result).isNotNull();
        assertThat(result.geometry()).isEqualTo("polyline_ok");
        verify(osrmClient, times(2)).getRoute(anyString(), anyString());
    }
}
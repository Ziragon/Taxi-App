package com.example.tripservice.integration;

import com.example.shared.exception.common.ServiceUnavailableException;
import com.example.tripservice.client.OsrmClient;
import com.example.tripservice.config.TestContainersConfig;
import com.example.tripservice.dto.data.RouteDto;
import com.example.tripservice.dto.response.OsrmResponse;
import com.example.tripservice.exception.RouteNotFoundException;
import com.example.tripservice.service.NavigationService;
import feign.FeignException;
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
import java.util.List;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Import(TestContainersConfig.class)
@ActiveProfiles("test")
class NavigationServiceIntegrationTest {

    @Autowired
    private NavigationService navigationService;

    @Autowired
    private CacheManager cacheManager;

    @MockitoBean
    private OsrmClient osrmClient;

    @BeforeEach
    void clearCaches() {
        cacheManager.getCacheNames()
                .forEach(name -> Objects.requireNonNull(cacheManager.getCache(name)).clear());
    }

    private OsrmResponse buildSuccessResponse(double distance, int duration, String geometry) {
        RouteDto route = new RouteDto(distance, duration, geometry);
        return new OsrmResponse("Ok", List.of(route));
    }

    private OsrmResponse buildFailResponse() {
        return new OsrmResponse("NoRoute", List.of());
    }

    private static final BigDecimal ORIGIN_LNG = new BigDecimal("37.62");
    private static final BigDecimal ORIGIN_LAT = new BigDecimal("55.75");
    private static final BigDecimal DEST_LNG   = new BigDecimal("37.70");
    private static final BigDecimal DEST_LAT   = new BigDecimal("55.80");

    @Test
    @DisplayName("Идентичные вызовы - кэш-хит")
    void getRouteInfo_sameCoordsCalledTwice_clientInvokedOnce() {
        when(osrmClient.getRoute(anyString(), anyString()))
                .thenReturn(buildSuccessResponse(5_000.0, 600, "polyline_abc"));

        RouteDto first  = navigationService.getRouteInfo(ORIGIN_LNG, ORIGIN_LAT, DEST_LNG, DEST_LAT);
        RouteDto second = navigationService.getRouteInfo(ORIGIN_LNG, ORIGIN_LAT, DEST_LNG, DEST_LAT);

        assertThat(first).isEqualTo(second);
        verify(osrmClient, times(1)).getRoute(anyString(), anyString());
    }

    @Test
    @DisplayName("Разные вызовы - вызов клиента дважды")
    void getRouteInfo_differentCoords_clientInvokedTwice() {
        when(osrmClient.getRoute(anyString(), anyString()))
                .thenReturn(buildSuccessResponse(5_000.0, 600,  "polyline_A"))
                .thenReturn(buildSuccessResponse(12_000.0, 900, "polyline_B"));

        RouteDto first = navigationService.getRouteInfo(
                ORIGIN_LNG, ORIGIN_LAT, DEST_LNG, DEST_LAT
        );
        RouteDto second = navigationService.getRouteInfo(
                new BigDecimal("30.32"), new BigDecimal("59.93"),
                new BigDecimal("30.40"), new BigDecimal("60.00")
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
                navigationService.getRouteInfo(ORIGIN_LNG, ORIGIN_LAT, DEST_LNG, DEST_LAT)
        ).isInstanceOf(RouteNotFoundException.class);
    }

    @Test
    @DisplayName("FeignException - ServiceUnavailableException")
    void getRouteInfo_feignException_throwsServiceUnavailableException() {
        when(osrmClient.getRoute(anyString(), anyString()))
                .thenThrow(mock(FeignException.class));

        assertThatThrownBy(() ->
                navigationService.getRouteInfo(ORIGIN_LNG, ORIGIN_LAT, DEST_LNG, DEST_LAT)
        ).isInstanceOf(ServiceUnavailableException.class);
    }

    @Test
    @DisplayName("Корректная выдача результата после Exception")
    void getRouteInfo_afterFailure_successfulRetryGoesToClient() {
        when(osrmClient.getRoute(anyString(), anyString()))
                .thenReturn(buildFailResponse())
                .thenReturn(buildSuccessResponse(5_000.0, 600, "polyline_ok"));

        assertThatThrownBy(() ->
                navigationService.getRouteInfo(ORIGIN_LNG, ORIGIN_LAT, DEST_LNG, DEST_LAT)
        ).isInstanceOf(RouteNotFoundException.class);

        RouteDto result = navigationService.getRouteInfo(ORIGIN_LNG, ORIGIN_LAT, DEST_LNG, DEST_LAT);

        assertThat(result).isNotNull();
        assertThat(result.geometry()).isEqualTo("polyline_ok");
        verify(osrmClient, times(2)).getRoute(anyString(), anyString());
    }
}
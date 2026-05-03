package com.example.tripservice.unit.service;

import com.example.shared.exception.common.ServiceUnavailableException;
import com.example.tripservice.client.OsrmClient;
import com.example.tripservice.dto.data.RouteDto;
import com.example.tripservice.dto.response.OsrmResponse;
import com.example.tripservice.exception.RouteNotFoundException;
import com.example.tripservice.service.external.NavigationService;
import feign.FeignException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OsrmNavigationServiceTest {

    @Mock
    private OsrmClient osrmClient;

    @InjectMocks
    private NavigationService navigationService;

    private static final BigDecimal ORIGIN_LNG = new BigDecimal("37.617600");
    private static final BigDecimal ORIGIN_LAT = new BigDecimal("55.755800");
    private static final BigDecimal DEST_LNG   = new BigDecimal("30.315800");
    private static final BigDecimal DEST_LAT   = new BigDecimal("59.939100");

    private RouteDto stubRoute;

    @BeforeEach
    void setUp() {
        stubRoute = new RouteDto(15000.0, 1200, "encoded_polyline_string");
    }

    @Nested
    @DisplayName("Успешные сценарии")
    class Success {

        @Test
        @DisplayName("code=Ok, маршруты не пусты → возвращается первый маршрут")
        void ok_withRoutes_returnsFirstRoute() {
            RouteDto secondRoute = new RouteDto(20000.0, 1800, "other_polyline");
            OsrmResponse response = new OsrmResponse("Ok", List.of(stubRoute, secondRoute));
            when(osrmClient.getRoute(any(), eq("full"))).thenReturn(response);

            RouteDto result = navigationService.getRouteInfo(ORIGIN_LNG, ORIGIN_LAT, DEST_LNG, DEST_LAT);

            assertThat(result).isEqualTo(stubRoute);
            assertThat(result.distance()).isEqualTo(15000.0);
            assertThat(result.duration()).isEqualTo(1200);
        }

        @Test
        @DisplayName("Клиент вызывается ровно один раз")
        void client_calledOnce() {
            OsrmResponse response = new OsrmResponse("Ok", List.of(stubRoute));
            when(osrmClient.getRoute(any(), any())).thenReturn(response);

            navigationService.getRouteInfo(ORIGIN_LNG, ORIGIN_LAT, DEST_LNG, DEST_LAT);

            verify(osrmClient, times(1)).getRoute(any(), eq("full"));
        }
    }

    @Nested
    @DisplayName("Формат координат")
    class CoordinateFormat {

        @Test
        @DisplayName("Координаты передаются в формате lng,lat;lng,lat")
        void coords_builtAsLngLatPairs() {
            OsrmResponse response = new OsrmResponse("Ok", List.of(stubRoute));
            ArgumentCaptor<String> coordsCaptor = ArgumentCaptor.forClass(String.class);
            when(osrmClient.getRoute(coordsCaptor.capture(), any())).thenReturn(response);

            navigationService.getRouteInfo(ORIGIN_LNG, ORIGIN_LAT, DEST_LNG, DEST_LAT);

            String captured = coordsCaptor.getValue();
            String expected = String.format(
                    java.util.Locale.US,
                    "%s,%s;%s,%s",
                    ORIGIN_LNG, ORIGIN_LAT, DEST_LNG, DEST_LAT
            );
            assertThat(captured).isEqualTo(expected);
        }

        @Test
        @DisplayName("Координаты используют точку как разделитель (Locale.US)")
        void coords_usesDotAsDecimalSeparator() {
            OsrmResponse response = new OsrmResponse("Ok", List.of(stubRoute));
            ArgumentCaptor<String> coordsCaptor = ArgumentCaptor.forClass(String.class);
            when(osrmClient.getRoute(coordsCaptor.capture(), any())).thenReturn(response);

            navigationService.getRouteInfo(ORIGIN_LNG, ORIGIN_LAT, DEST_LNG, DEST_LAT);

            assertThat(coordsCaptor.getValue()).doesNotContain(",".repeat(5));
            assertThat(coordsCaptor.getValue()).contains(".");
        }

        @Test
        @DisplayName("overview=full всегда передаётся в клиент")
        void overview_fullPassedToClient() {
            OsrmResponse response = new OsrmResponse("Ok", List.of(stubRoute));
            when(osrmClient.getRoute(any(), eq("full"))).thenReturn(response);

            navigationService.getRouteInfo(ORIGIN_LNG, ORIGIN_LAT, DEST_LNG, DEST_LAT);

            verify(osrmClient).getRoute(any(), eq("full"));
        }
    }

    @Nested
    @DisplayName("Ошибочные сценарии")
    class ErrorScenarios {

        @Test
        @DisplayName("code != Ok - RouteNotFoundException")
        void nonOkCode_throwsRouteNotFoundException() {
            OsrmResponse response = new OsrmResponse("NoRoute", List.of());
            when(osrmClient.getRoute(any(), any())).thenReturn(response);

            assertThatThrownBy(() ->
                    navigationService.getRouteInfo(ORIGIN_LNG, ORIGIN_LAT, DEST_LNG, DEST_LAT)
            ).isInstanceOf(RouteNotFoundException.class);
        }

        @Test
        @DisplayName("code=Ok, routes пустой - RouteNotFoundException")
        void okCode_emptyRoutes_throwsRouteNotFoundException() {
            OsrmResponse response = new OsrmResponse("Ok", List.of());
            when(osrmClient.getRoute(any(), any())).thenReturn(response);

            assertThatThrownBy(() ->
                    navigationService.getRouteInfo(ORIGIN_LNG, ORIGIN_LAT, DEST_LNG, DEST_LAT)
            ).isInstanceOf(RouteNotFoundException.class);
        }

        @Test
        @DisplayName("FeignException - ServiceUnavailableException с именем OSRM")
        void feignException_throwsServiceUnavailableException() {
            when(osrmClient.getRoute(any(), any()))
                    .thenThrow(mock(FeignException.class));

            assertThatThrownBy(() ->
                    navigationService.getRouteInfo(ORIGIN_LNG, ORIGIN_LAT, DEST_LNG, DEST_LAT)
            )
                    .isInstanceOf(ServiceUnavailableException.class)
                    .hasMessageContaining("OSRM");
        }

        @Test
        @DisplayName("FeignException не оборачивается в RouteNotFoundException")
        void feignException_notWrappedInRouteNotFound() {
            when(osrmClient.getRoute(any(), any()))
                    .thenThrow(mock(FeignException.class));

            assertThatThrownBy(() ->
                    navigationService.getRouteInfo(ORIGIN_LNG, ORIGIN_LAT, DEST_LNG, DEST_LAT)
            ).isNotInstanceOf(RouteNotFoundException.class);
        }
    }
}

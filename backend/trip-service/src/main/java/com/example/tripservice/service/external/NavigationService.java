package com.example.tripservice.service.external;

import com.example.shared.exception.common.ServiceUnavailableException;
import com.example.tripservice.client.OsrmClient;
import com.example.tripservice.dto.data.RouteDto;
import com.example.tripservice.dto.response.OsrmResponse;
import com.example.tripservice.exception.RouteNotFoundException;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class NavigationService {

    private final OsrmClient osrmClient;

    // Кэширует точный маршрут по координатам
    // Нужен если пользователь несколько раз случайно запросил один и тот же маршрут (несколько раз нажал подтвердить)
    @Cacheable(value = "routes",
            key = "{#originLng, #originLat, #destLng, #destLat}",
            unless = "#result == null")
    public RouteDto getRouteInfo(BigDecimal originLng, BigDecimal originLat,
                                 BigDecimal destLng, BigDecimal destLat
    ) {
        try {
            String coords = String.format(
                    java.util.Locale.US,
                    "%s,%s;%s,%s",
                    originLng, originLat, destLng, destLat
            );

            OsrmResponse response = osrmClient.getRoute(coords, "full");

            if (!response.code().equals("Ok")) {
                log.warn("OSRM API error: {}", response.code());
                throw new RouteNotFoundException(coords);
            }

            return response.routes().stream()
                    .findFirst()
                    .orElseThrow(() -> new RouteNotFoundException(coords));
        } catch (FeignException e) {
            log.error("Failed to fetch route info: {}", e.getMessage());
            throw new ServiceUnavailableException("OSRM");
        }
    }
}

package com.example.tripservice.dto.response;

import com.example.tripservice.dto.data.TripDto;
import com.example.tripservice.entity.enums.TripStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;

public record TripResponse(

        @Schema(description = "ID поездки", example = "1024")
        Long id,

        @Schema(description = "ID пассажира", example = "55")
        Long passengerId,

        @Schema(description = "Статус поездки", example = "SEARCHING")
        TripStatus status,

        @Schema(description = "Адрес подачи", example = "ул. Ильича, 4")
        String originAddress,

        @Schema(description = "Широта подачи", example = "54.8427")
        BigDecimal originLat,

        @Schema(description = "Долгота подачи", example = "83.0916")
        BigDecimal originLng,

        @Schema(description = "Адрес назначения", example = "Аэропорт Толмачево")
        String destAddress,

        @Schema(description = "Широта назначения", example = "55.0089")
        BigDecimal destLat,

        @Schema(description = "Долгота назначения", example = "82.6672")
        BigDecimal destLng,

        @Schema(description = "Расстояние (км)", example = "35.2")
        BigDecimal distanceKm,

        @Schema(description = "Длительность (мин)", example = "45.0")
        BigDecimal durationMin,

        @Schema(description = "Коэффициент погоды", example = "1.2")
        BigDecimal weatherCoef,

        @Schema(description = "Коэффициент спроса (Surge)", example = "1.1")
        BigDecimal surgeCoef,

        @Schema(description = "Список доступных тарифов")
        List<TariffResponse> tariffs,

        @Schema(description = "Геометрия маршрута")
        String routeGeometry
) {
    public static TripResponse from(TripDto trip) {
        List<TariffResponse> tariffs = trip.tariffDtos() == null
                ? List.of()
                : trip.tariffDtos().stream()
                  .map(TariffResponse::from)
                  .toList();

        return new TripResponse(
                trip.id(),
                trip.passengerId(),
                trip.status(),
                trip.originAddress(),
                trip.originLat(),
                trip.originLng(),
                trip.destAddress(),
                trip.destLat(),
                trip.destLng(),
                trip.distanceKm(),
                trip.durationMin(),
                trip.weatherCoef(),
                trip.surgeCoef(),
                tariffs,
                trip.routeGeometry()
        );
    }
}

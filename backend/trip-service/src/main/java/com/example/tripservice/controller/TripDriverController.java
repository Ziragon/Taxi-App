package com.example.tripservice.controller;

import com.example.shared.security.UserPrincipal;
import com.example.tripservice.dto.data.RouteDto;
import com.example.tripservice.dto.request.DriverCoordinatesRequest;
import com.example.tripservice.dto.response.RouteResponse;
import com.example.tripservice.dto.response.TripResponse;
import com.example.tripservice.service.trip.TripDriverService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/v1/trips")
@RequiredArgsConstructor
public class TripDriverController {

    private final TripDriverService tripDriverService;

    @PostMapping("/{tripId}/accept")
    @Operation(
            summary = "Принять заказ",
            description = "Водитель подтверждает принятие заказа. Возвращается маршрут до точки подачи",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "latitude": 55.0302,
                                      "longitude": 82.9204
                                    }
                                    """)
                    )
            ),
            responses = {
                    @ApiResponse(responseCode = "200", description = "Заказ принят, маршрут рассчитан"),
                    @ApiResponse(responseCode = "409", description = "Заказ уже принят другим водителем или отменен")
            }
    )
    public ResponseEntity<RouteResponse> acceptTrip(
            @PathVariable Long tripId,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody DriverCoordinatesRequest request) {
        RouteDto route = tripDriverService.acceptTrip(
                tripId, principal.userId(), request.longitude(), request.latitude());
        return ResponseEntity.ok(RouteResponse.from(route));
    }

    @PostMapping("/{tripId}/reject")
    @Operation(
            summary = "Отклонить заказ",
            description = "Водитель отклоняет заказ. Заказ продолжает искаться",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Поездка начата")
            }
    )
    public ResponseEntity<Void> rejectTrip(
            @PathVariable Long tripId,
            @AuthenticationPrincipal UserPrincipal principal) {
        tripDriverService.rejectTrip(tripId, principal.userId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{tripId}/start")
    @Operation(
            summary = "Начать поездку",
            description = "Отметка о том, что пассажир в машине. Статус меняется на IN_PROGRESS",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Поездка начата")
            }
    )
    public ResponseEntity<RouteResponse> startTrip(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long tripId
    ) {
        RouteDto route = tripDriverService.startTrip(tripId, principal.userId());
        return ResponseEntity.ok(RouteResponse.from(route));
    }

    @PostMapping("/{tripId}/complete")
    @Operation(
            summary = "Завершить поездку",
            description = "Фиксация прибытия в конечную точку",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Поездка успешно завершена")
            }
    )
    public ResponseEntity<Void> completeTrip(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long tripId
    ) {
        tripDriverService.completeTrip(tripId, principal.userId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/driver-active")
    @Operation(
            summary = "Получить информацию об активной поездке водителя",
            description = "Возвращает детали активной поездки (если она есть), нужно при перезаходе в приложение",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Данные получены"),
                    @ApiResponse(responseCode = "204", description = "Активной поездки нету, можно искать новые предложения")
            }
    )
    public ResponseEntity<TripResponse> getActiveTrip(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody DriverCoordinatesRequest request
    ) {
        return Optional.ofNullable(tripDriverService.getActiveTrip(principal.userId(), request.latitude(), request.latitude()))
                .map(trip -> ResponseEntity.ok(TripResponse.from(trip)))
                .orElseGet(() -> ResponseEntity.noContent().build());
    }
}

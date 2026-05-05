package com.example.tripservice.controller;

import com.example.shared.dto.enums.VehicleClass;
import com.example.shared.security.UserPrincipal;
import com.example.tripservice.dto.data.TripCreateDto;
import com.example.tripservice.dto.data.TripDto;
import com.example.tripservice.dto.request.TripCreateRequest;
import com.example.tripservice.dto.response.TripResponse;
import com.example.tripservice.service.trip.TripPassengerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/v1/trips")
@RequiredArgsConstructor
@Tag(name = "Passenger Trip API", description = "Управление поездками со стороны пассажира")
public class TripPassengerController {

    private final TripPassengerService tripPassengerService;

    @PostMapping
    @Operation(
            summary = "Предварительный расчет поездки",
            description = "Создает черновик поездки, рассчитывает дистанцию, время и возвращает список доступных тарифов",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "originAddress": "ул. Ильича, 4",
                                      "originLat": 54.8427,
                                      "originLng": 83.0916,
                                      "destAddress": "Красный проспект, 36",
                                      "destLat": 55.0302,
                                      "destLng": 82.9204
                                    }
                                    """)
                    )
            ),
            responses = {
                    @ApiResponse(responseCode = "200", description = "Расчет выполнен успешно"),
                    @ApiResponse(responseCode = "400", description = "Некорректные координаты или адрес")
            }
    )
    public ResponseEntity<TripResponse> createTrip(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody TripCreateRequest request
    ) {
        TripDto result = tripPassengerService.createTrip(
                principal.userId(), TripCreateDto.from(request));
        return ResponseEntity.ok(TripResponse.from(result));
    }

    @PostMapping("/start-search")
    @Operation(
            summary = "Начать поиск водителя",
            description = "Выбор конкретного тарифа и перевод поездки в статус поиска водителя",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Поиск запущен"),
                    @ApiResponse(responseCode = "410", description = "Поездка просрочена или не существует (нужно создать новую)")
            }
    )
    public ResponseEntity<TripResponse> startSearching(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam VehicleClass vehicleClass
    ) {
        TripDto result = tripPassengerService.startSearching(principal.userId(), vehicleClass);
        return ResponseEntity.ok(TripResponse.from(result));
    }

    @PostMapping("/{tripId}/cancel")
    @Operation(
            summary = "Отмена поездки",
            description = "Позволяет пассажиру отменить поездку на этапе поиска и когда водитель едет к пассажиру",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Поездка успешно отменена"),
                    @ApiResponse(responseCode = "422", description = "Невозможно отменить поездку когда она уже в процессе")
            }
    )
    public ResponseEntity<Void> cancelTrip(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long tripId
    ) {
        tripPassengerService.cancelTrip(tripId, principal.userId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{tripId}")
    @Operation(
            summary = "Получить информацию о поездке",
            description = "Возвращает текущие детали поездки, включая статус и данные водителя (если назначен)",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Данные получены"),
                    @ApiResponse(responseCode = "404", description = "Поездка не найдена")
            }
    )
    public ResponseEntity<TripResponse> getTrip(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long tripId
    ) {
        return ResponseEntity.ok(
                TripResponse.from(tripPassengerService.getTrip(principal.userId(), tripId)));
    }

    @GetMapping("/active")
    @Operation(
            summary = "Получить информацию об активной поездке",
            description = "Возвращает детали активной поездки (если она есть), нужно при перезаходе в приложение",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Данные получены"),
                    @ApiResponse(responseCode = "204", description = "Активной поездки нету, можно создавать новую")
            }
    )
    public ResponseEntity<TripResponse> getActiveTrip(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return Optional.ofNullable(tripPassengerService.getActiveTrip(principal.userId()))
                .map(trip -> ResponseEntity.ok(TripResponse.from(trip)))
                .orElseGet(() -> ResponseEntity.noContent().build());
    }
}

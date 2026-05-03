package com.example.tripservice.controller;

import com.example.shared.security.UserPrincipal;
import com.example.tripservice.dto.data.RouteDto;
import com.example.tripservice.dto.response.DriverCoordinatesRequest;
import com.example.tripservice.service.DriverSearchService;
import com.example.tripservice.service.TripService;
import com.example.tripservice.service.TripStatusService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/trips")
@RequiredArgsConstructor
public class TripDriverController {

    private final TripStatusService tripStatusService;
    private final DriverSearchService driverSearchService;
    private final TripService tripService;

    @PostMapping("/{tripId}/accept")
    @Operation(
            summary = "Принять поездку",
            description = "Принимает поездку по активному предложению водителю",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Водитель назначен"),
                    @ApiResponse(responseCode = "403", description = "Пользователь не является водителем или предложение для водителя не существует")
            }
    )
    public ResponseEntity<RouteDto> acceptTrip(
            @PathVariable Long tripId,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody DriverCoordinatesRequest request
    ) {
        driverSearchService.handleDriverAccept(tripId, principal.userId());
        RouteDto route = tripService.getRouteToPassenger(
                tripId, request.longitude(), request.latitude()
        );
        return ResponseEntity.ok(route);
    }

    @PostMapping("/{tripId}/reject")
    @Operation(
            summary = "Отклонить поездку",
            description = "Отклоняет предложение поездки",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Предложение отклонено"),
                    @ApiResponse(responseCode = "403", description = "Пользователь не является водителем или предложение для водителя не существует")
            }
    )
    public ResponseEntity<Void> rejectTrip(
            @PathVariable Long tripId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        driverSearchService.handleDriverReject(tripId, principal.userId());

        return ResponseEntity.noContent().build();
    }


    @PostMapping("/{tripId}/start")
    @Operation(
            summary = "Начать поездку",
            description = "Водитель подбирает пассажира и начинает поездку",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Поездка начата"),
                    @ApiResponse(responseCode = "403", description = "Пользователь не является водителем")
            }
    )
    public ResponseEntity<Void> startTrip(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long tripId
    ) {
        tripStatusService.startTrip(tripId, principal.userId());

        return ResponseEntity.ok().build();
    }

    @PostMapping("/{tripId}/complete")
    public ResponseEntity<Void> completeTrip(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long tripId
    ) {
        tripStatusService.completeTrip(tripId, principal.userId());

        return ResponseEntity.ok().build();
    }
}

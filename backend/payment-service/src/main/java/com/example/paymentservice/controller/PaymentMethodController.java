package com.example.paymentservice.controller;

import com.example.paymentservice.dto.request.AddPaymentMethodRequest;
import com.example.paymentservice.dto.response.PaymentMethodResponse;
import com.example.paymentservice.entity.PaymentMethod;
import com.example.paymentservice.service.PaymentMethodService;
import com.example.shared.security.UserPrincipal;
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

import java.util.List;

@RestController
@RequestMapping("/api/v1/payment-methods")
@RequiredArgsConstructor
@Tag(name = "Payment Methods", description = "Управление платёжными методами пассажира")
public class PaymentMethodController {

    private final PaymentMethodService paymentMethodService;

    @PostMapping
    @Operation(
            summary = "Добавить платёжный метод",
            description = "Добавляет карту через Stripe Payment Method ID. Первая карта автоматически становится основной",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "stripePaymentMethodId": "pm_1234567890",
                                      "setAsDefault": false
                                    }
                                    """)
                    )
            ),
            responses = {
                    @ApiResponse(responseCode = "200", description = "Платёжный метод добавлен"),
                    @ApiResponse(responseCode = "409", description = "Карта уже добавлена"),
                    @ApiResponse(responseCode = "422", description = "Невалидный запрос")
            }
    )
    public ResponseEntity<PaymentMethodResponse> addPaymentMethod(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody AddPaymentMethodRequest request
    ) {
        PaymentMethod paymentMethod = paymentMethodService.addPaymentMethod(
                principal.userId(),
                request.stripePaymentMethodId(),
                request.setAsDefault()
        );

        return ResponseEntity.ok(PaymentMethodResponse.from(paymentMethod));
    }

    @GetMapping
    @Operation(
            summary = "Получить все активные платёжные методы",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Список платёжных методов")
            }
    )
    public ResponseEntity<List<PaymentMethodResponse>> getPaymentMethods(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        List<PaymentMethod> methods = paymentMethodService.getActiveByPassengerId(principal.userId());

        return ResponseEntity.ok(
                methods.stream()
                        .map(PaymentMethodResponse::from)
                        .toList()
        );
    }

    @GetMapping("/default")
    @Operation(
            summary = "Получить основной платёжный метод",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Основной платёжный метод"),
                    @ApiResponse(responseCode = "404", description = "Основной метод не найден")
            }
    )
    public ResponseEntity<PaymentMethodResponse> getDefaultPaymentMethod(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        PaymentMethod method = paymentMethodService.getDefaultForPassenger(principal.userId());

        return ResponseEntity.ok(PaymentMethodResponse.from(method));
    }

    @PutMapping("/{paymentMethodId}/set-default")
    @Operation(
            summary = "Установить платёжный метод основным",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Платёжный метод установлен основным"),
                    @ApiResponse(responseCode = "404", description = "Платёжный метод не найден"),
                    @ApiResponse(responseCode = "400", description = "Платёжный метод неактивен")
            }
    )
    public ResponseEntity<Void> setDefaultPaymentMethod(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long paymentMethodId
    ) {
        paymentMethodService.setDefault(principal.userId(), paymentMethodId);

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{paymentMethodId}")
    @Operation(
            summary = "Удалить платёжный метод",
            description = "Деактивирует платёжный метод. Нельзя удалить основной метод",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Платёжный метод удалён"),
                    @ApiResponse(responseCode = "404", description = "Платёжный метод не найден"),
                    @ApiResponse(responseCode = "400", description = "Нельзя удалить основной метод")
            }
    )
    public ResponseEntity<Void> deletePaymentMethod(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long paymentMethodId
    ) {
        paymentMethodService.deactivate(principal.userId(), paymentMethodId);

        return ResponseEntity.noContent().build();
    }
}

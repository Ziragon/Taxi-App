package com.example.paymentservice.controller.internal;

import com.example.paymentservice.dto.response.PaymentMethodResponse;
import com.example.paymentservice.entity.PaymentMethod;
import com.example.paymentservice.service.PaymentMethodService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/internal/payment-methods")
@RequiredArgsConstructor
@Tag(name = "Internal Payment Methods", description = "Внутренний API для платёжных методов")
public class InternalPaymentMethodController {

    private final PaymentMethodService paymentMethodService;

    @GetMapping("/passenger/{passengerId}/default")
    @Operation(
            summary = "Получить дефолтный платёжный метод пассажира (внутренний)",
            description = "Вызывается Trip Service для получения платёжного метода перед созданием поездки",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Платёжный метод найден"),
                    @ApiResponse(responseCode = "404", description = "Платёжный метод не найден")
            }
    )
    public ResponseEntity<PaymentMethodResponse> getDefaultPaymentMethod(
            @PathVariable Long passengerId
    ) {
        PaymentMethod method = paymentMethodService.getDefaultForPassenger(passengerId);

        return ResponseEntity.ok(PaymentMethodResponse.from(method));
    }

    @GetMapping("/{paymentMethodId}")
    @Operation(
            summary = "Получить платёжный метод по ID (внутренний)",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Платёжный метод найден"),
                    @ApiResponse(responseCode = "404", description = "Платёжный метод не найден")
            }
    )
    public ResponseEntity<PaymentMethodResponse> getPaymentMethod(
            @PathVariable Long paymentMethodId
    ) {
        PaymentMethod method = paymentMethodService.getById(paymentMethodId);

        return ResponseEntity.ok(PaymentMethodResponse.from(method));
    }
}

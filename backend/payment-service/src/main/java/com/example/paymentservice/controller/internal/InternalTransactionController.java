package com.example.paymentservice.controller.internal;

import com.example.paymentservice.dto.request.CreateChargeRequest;
import com.example.paymentservice.dto.request.CreatePayoutRequest;
import com.example.paymentservice.dto.request.CreateRefundRequest;
import com.example.paymentservice.dto.response.TransactionResponse;
import com.example.paymentservice.entity.Transaction;
import com.example.paymentservice.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/internal/transactions")
@RequiredArgsConstructor
@Tag(name = "Internal Transactions", description = "Внутренний API для межсервисного взаимодействия")
public class InternalTransactionController {

    private final TransactionService transactionService;

    @PostMapping("/charge")
    @Operation(
            summary = "Создать платёж (внутренний)",
            description = "Вызывается Trip Service после завершения поездки",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "tripId": 123,
                                      "passengerId": 456,
                                      "driverId": 789,
                                      "paymentMethodId": null,
                                      "amount": 25.50,
                                      "currency": "usd"
                                    }
                                    """)
                    )
            ),
            responses = {
                    @ApiResponse(responseCode = "200", description = "Платёж создан"),
                    @ApiResponse(responseCode = "404", description = "Платёжный метод не найден"),
                    @ApiResponse(responseCode = "422", description = "Ошибка валидации")
            }
    )
    public ResponseEntity<TransactionResponse> createCharge(
            @Valid @RequestBody CreateChargeRequest request
    ) {
        Transaction transaction = transactionService.createCharge(
                request.tripId(),
                request.passengerId(),
                request.driverId(),
                request.paymentMethodId(),
                request.amount(),
                request.currency()
        );

        return ResponseEntity.ok(TransactionResponse.from(transaction));
    }

    @PostMapping("/refund")
    @Operation(
            summary = "Создать возврат (внутренний)",
            description = "Вызывается Trip Service при отмене поездки",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "transactionId": 1,
                                      "amount": 25.50,
                                      "reason": "Trip cancelled by driver"
                                    }
                                    """)
                    )
            ),
            responses = {
                    @ApiResponse(responseCode = "200", description = "Возврат создан"),
                    @ApiResponse(responseCode = "404", description = "Транзакция не найдена"),
                    @ApiResponse(responseCode = "400", description = "Транзакция не может быть возвращена")
            }
    )
    public ResponseEntity<TransactionResponse> createRefund(
            @Valid @RequestBody CreateRefundRequest request
    ) {
        Transaction refund = transactionService.createRefund(
                request.transactionId(),
                request.amount(),
                request.reason()
        );

        return ResponseEntity.ok(TransactionResponse.from(refund));
    }

    @PostMapping("/payout")
    @Operation(
            summary = "Создать выплату водителю (внутренний)",
            description = "Вызывается после успешного завершения поездки для выплаты водителю",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "tripId": 123,
                                      "passengerId": 456,
                                      "driverId": 789,
                                      "amount": 20.00,
                                      "currency": "usd"
                                    }
                                    """)
                    )
            ),
            responses = {
                    @ApiResponse(responseCode = "200", description = "Выплата создана"),
                    @ApiResponse(responseCode = "404", description = "Счёт водителя не найден"),
                    @ApiResponse(responseCode = "400", description = "Счёт водителя не верифицирован")
            }
    )
    public ResponseEntity<TransactionResponse> createPayout(
            @Valid @RequestBody CreatePayoutRequest request
    ) {
        Transaction payout = transactionService.createPayout(
                request.tripId(),
                request.passengerId(),
                request.driverId(),
                request.amount(),
                request.currency()
        );

        return ResponseEntity.ok(TransactionResponse.from(payout));
    }

    @GetMapping("/trip/{tripId}")
    @Operation(
            summary = "Получить транзакцию по ID поездки (внутренний)",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Транзакция найдена"),
                    @ApiResponse(responseCode = "404", description = "Транзакция не найдена")
            }
    )
    public ResponseEntity<TransactionResponse> getByTripId(
            @PathVariable Long tripId
    ) {
        Transaction transaction = transactionService.getByTripId(tripId);

        return ResponseEntity.ok(TransactionResponse.from(transaction));
    }
}

package com.example.paymentservice.controller;

import com.example.paymentservice.dto.request.CreateChargeRequest;
import com.example.paymentservice.dto.request.CreateRefundRequest;
import com.example.paymentservice.dto.response.TransactionResponse;
import com.example.paymentservice.entity.Transaction;
import com.example.paymentservice.service.TransactionService;
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
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
@Tag(name = "Transactions", description = "Управление транзакциями и платежами")
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping("/charge")
    @Operation(
            summary = "Создать платёж",
            description = "Списывает средства с пассажира через Stripe. Если paymentMethodId не указан — используется дефолтный метод",
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
            summary = "Создать возврат средств",
            description = "Возврат средств пассажиру через Stripe. Только для транзакций со статусом SUCCEEDED",
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

    @GetMapping("/{id}")
    @Operation(
            summary = "Получить транзакцию по ID",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Транзакция найдена"),
                    @ApiResponse(responseCode = "404", description = "Транзакция не найдена")
            }
    )
    public ResponseEntity<TransactionResponse> getTransaction(
            @PathVariable Long id
    ) {
        Transaction transaction = transactionService.getById(id);

        return ResponseEntity.ok(TransactionResponse.from(transaction));
    }

    @GetMapping("/trip/{tripId}")
    @Operation(
            summary = "Получить транзакцию по ID поездки",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Транзакция найдена"),
                    @ApiResponse(responseCode = "404", description = "Транзакция не найдена")
            }
    )
    public ResponseEntity<TransactionResponse> getTransactionByTripId(
            @PathVariable Long tripId
    ) {
        Transaction transaction = transactionService.getByTripId(tripId);

        return ResponseEntity.ok(TransactionResponse.from(transaction));
    }

    @GetMapping("/history/passenger")
    @Operation(
            summary = "История транзакций пассажира",
            responses = {
                    @ApiResponse(responseCode = "200", description = "История транзакций")
            }
    )
    public ResponseEntity<List<TransactionResponse>> getPassengerHistory(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        List<Transaction> transactions = transactionService.getPassengerHistory(principal.userId());

        return ResponseEntity.ok(
                transactions.stream()
                        .map(TransactionResponse::from)
                        .toList()
        );
    }

    @GetMapping("/history/driver")
    @Operation(
            summary = "История транзакций водителя",
            responses = {
                    @ApiResponse(responseCode = "200", description = "История транзакций")
            }
    )
    public ResponseEntity<List<TransactionResponse>> getDriverHistory(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        List<Transaction> transactions = transactionService.getDriverHistory(principal.userId());

        return ResponseEntity.ok(
                transactions.stream()
                        .map(TransactionResponse::from)
                        .toList()
        );
    }
}

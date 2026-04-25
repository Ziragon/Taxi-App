package com.example.paymentservice.controller.mock;

import com.example.paymentservice.dto.response.TransactionResponse;
import com.example.paymentservice.entity.Transaction;
import com.example.paymentservice.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/v1/mock/transactions")
@RequiredArgsConstructor
@Profile("local")
@Tag(name = "Mock Transactions", description = "Мок эндпоинты для тестирования без Trip Service")
public class MockTransactionController {

    private final TransactionService transactionService;

    @PostMapping("/charge")
    @Operation(
            summary = "Мок: Создать платёж",
            description = "Создаёт тестовый платёж с заранее заданными данными поездки",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "passengerId": 1,
                                      "paymentMethodId": null,
                                      "amount": 25.50
                                    }
                                    """)
                    )
            ),
            responses = {
                    @ApiResponse(responseCode = "200", description = "Мок платёж создан")
            }
    )
    public ResponseEntity<TransactionResponse> mockCharge(
            @RequestParam(required = false) Long passengerId,
            @RequestParam(required = false) Long driverId,
            @RequestParam(required = false) Long paymentMethodId,
            @RequestParam(defaultValue = "25.50") BigDecimal amount
    ) {
        Transaction transaction = transactionService.createCharge(
                1L,
                passengerId != null ? passengerId : 1L,
                driverId != null ? driverId : 2L,
                paymentMethodId,
                amount,
                "usd"
        );

        return ResponseEntity.ok(TransactionResponse.from(transaction));
    }

    @PostMapping("/refund/{transactionId}")
    @Operation(
            summary = "Мок: Создать возврат",
            description = "Создаёт тестовый возврат для указанной транзакции",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Мок возврат создан"),
                    @ApiResponse(responseCode = "404", description = "Транзакция не найдена"),
                    @ApiResponse(responseCode = "400", description = "Транзакция не может быть возвращена")
            }
    )
    public ResponseEntity<TransactionResponse> mockRefund(
            @PathVariable Long transactionId,
            @RequestParam(defaultValue = "25.50") BigDecimal amount
    ) {
        Transaction refund = transactionService.createRefund(
                transactionId,
                amount,
                "Mock refund for testing"
        );

        return ResponseEntity.ok(TransactionResponse.from(refund));
    }

    @PostMapping("/payout")
    @Operation(
            summary = "Мок: Создать выплату водителю",
            description = "Создаёт тестовую выплату водителю",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Мок выплата создана"),
                    @ApiResponse(responseCode = "404", description = "Счёт водителя не найден"),
                    @ApiResponse(responseCode = "400", description = "Счёт водителя не верифицирован")
            }
    )
    public ResponseEntity<TransactionResponse> mockPayout(
            @RequestParam(required = false) Long driverId,
            @RequestParam(defaultValue = "20.00") BigDecimal amount
    ) {
        Transaction payout = transactionService.createPayout(
                1L,
                1L,
                driverId != null ? driverId : 2L,
                amount,
                "usd"
        );

        return ResponseEntity.ok(TransactionResponse.from(payout));
    }

    @GetMapping("/trip/{tripId}")
    @Operation(
            summary = "Мок: Получить транзакцию по ID поездки",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Транзакция найдена"),
                    @ApiResponse(responseCode = "404", description = "Транзакция не найдена")
            }
    )
    public ResponseEntity<TransactionResponse> mockGetByTripId(
            @PathVariable Long tripId
    ) {
        Transaction transaction = transactionService.getByTripId(tripId);

        return ResponseEntity.ok(TransactionResponse.from(transaction));
    }
}

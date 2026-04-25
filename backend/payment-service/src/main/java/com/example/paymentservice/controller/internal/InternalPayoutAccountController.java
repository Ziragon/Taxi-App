package com.example.paymentservice.controller.internal;

import com.example.paymentservice.dto.response.DriverPayoutAccountResponse;
import com.example.paymentservice.entity.DriverPayoutAccount;
import com.example.paymentservice.service.DriverPayoutAccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/internal/payout-accounts")
@RequiredArgsConstructor
@Tag(name = "Internal Payout Accounts", description = "Внутренний API для счетов выплат")
public class InternalPayoutAccountController {

    private final DriverPayoutAccountService payoutAccountService;

    @GetMapping("/driver/{driverId}/default")
    @Operation(
            summary = "Получить дефолтный счёт водителя (внутренний)",
            description = "Вызывается для проверки наличия верифицированного счёта перед созданием поездки",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Счёт найден"),
                    @ApiResponse(responseCode = "404", description = "Счёт не найден"),
                    @ApiResponse(responseCode = "400", description = "Счёт не верифицирован")
            }
    )
    public ResponseEntity<DriverPayoutAccountResponse> getVerifiedDefaultAccount(
            @PathVariable Long driverId
    ) {
        DriverPayoutAccount account = payoutAccountService.getVerifiedDefaultForDriver(driverId);

        return ResponseEntity.ok(DriverPayoutAccountResponse.from(account));
    }

    @GetMapping("/driver/{driverId}/has-verified")
    @Operation(
            summary = "Проверить наличие верифицированного счёта (внутренний)",
            description = "Вызывается Trip Service для проверки готовности водителя к приёму выплат",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Результат проверки")
            }
    )
    public ResponseEntity<Boolean> hasVerifiedAccount(
            @PathVariable Long driverId
    ) {
        boolean hasVerified = payoutAccountService.getAllByDriverId(driverId)
                .stream()
                .anyMatch(DriverPayoutAccount::isVerified);

        return ResponseEntity.ok(hasVerified);
    }
}

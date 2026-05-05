package com.example.paymentservice.controller;

import com.example.paymentservice.dto.request.AddPayoutAccountRequest;
import com.example.paymentservice.dto.response.DriverPayoutAccountResponse;
import com.example.paymentservice.entity.DriverPayoutAccount;
import com.example.paymentservice.service.DriverPayoutAccountService;
import com.example.shared.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/payout-accounts")
@RequiredArgsConstructor
@Tag(name = "Payout Accounts", description = "Управление счетами для выплат водителям")
public class DriverPayoutAccountController {

    private final DriverPayoutAccountService payoutAccountService;

    @PostMapping
    @Operation(
            summary = "Добавить счёт для выплат",
            description = "Создаёт Stripe Connect аккаунт для водителя. Первый счёт автоматически становится основным",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "lastFour": "4242"
                                    }
                                    """)
                    )
            ),
            responses = {
                    @ApiResponse(responseCode = "200", description = "Счёт добавлен"),
                    @ApiResponse(responseCode = "409", description = "Счёт уже добавлен")
            }
    )
    public ResponseEntity<DriverPayoutAccountResponse> addPayoutAccount(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody AddPayoutAccountRequest request
    ) {
        DriverPayoutAccount account = payoutAccountService.addPayoutAccount(
                principal.userId(),
                request.lastFour()
        );

        return ResponseEntity.ok(DriverPayoutAccountResponse.from(account));
    }

    @DeleteMapping("/{accountId}")
    @Operation(
            summary = "Удалить счёт для выплат",
            description = "Физически удаляет счёт. Нельзя удалить дефолтный если есть другие счета",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Счёт удалён"),
                    @ApiResponse(responseCode = "404", description = "Счёт не найден"),
                    @ApiResponse(responseCode = "400", description = "Нельзя удалить дефолтный счёт")
            }
    )
    public ResponseEntity<Void> deletePayoutAccount(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long accountId
    ) {
        payoutAccountService.delete(principal.userId(), accountId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    @Operation(
            summary = "Получить все счета водителя",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Список счетов")
            }
    )
    public ResponseEntity<List<DriverPayoutAccountResponse>> getPayoutAccounts(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        List<DriverPayoutAccount> accounts = payoutAccountService.getAllByDriverId(principal.userId());

        return ResponseEntity.ok(
                accounts.stream()
                        .map(DriverPayoutAccountResponse::from)
                        .toList()
        );
    }

    @GetMapping("/default")
    @Operation(
            summary = "Получить основной счёт для выплат",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Основной счёт"),
                    @ApiResponse(responseCode = "404", description = "Основной счёт не найден")
            }
    )
    public ResponseEntity<DriverPayoutAccountResponse> getDefaultPayoutAccount(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        DriverPayoutAccount account = payoutAccountService.getDefaultForDriver(principal.userId());

        return ResponseEntity.ok(DriverPayoutAccountResponse.from(account));
    }

    @PutMapping("/{accountId}/set-default")
    @Operation(
            summary = "Установить счёт основным",
            description = "Счёт должен быть верифицирован в Stripe",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Счёт установлен основным"),
                    @ApiResponse(responseCode = "404", description = "Счёт не найден"),
                    @ApiResponse(responseCode = "400", description = "Счёт не верифицирован")
            }
    )
    public ResponseEntity<Void> setDefaultPayoutAccount(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long accountId
    ) {
        payoutAccountService.setDefault(principal.userId(), accountId);

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{accountId}/verify")
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(
            summary = "Верифицировать счёт (только для админа)",
            description = "Проверяет статус верификации в Stripe и устанавливает is_verified = true",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Счёт верифицирован"),
                    @ApiResponse(responseCode = "400", description = "Счёт не верифицирован в Stripe"),
                    @ApiResponse(responseCode = "403", description = "Недостаточно прав")
            }
    )
    public ResponseEntity<Void> verifyPayoutAccount(
            @PathVariable Long accountId
    ) {
        payoutAccountService.verify(accountId);

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{accountId}/sync")
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(
            summary = "Синхронизировать статус верификации со Stripe (только для админа)",
            description = "Обновляет статус верификации счёта согласно актуальным данным из Stripe",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Статус синхронизирован"),
                    @ApiResponse(responseCode = "403", description = "Недостаточно прав")
            }
    )
    public ResponseEntity<Void> syncVerificationStatus(
            @PathVariable Long accountId
    ) {
        payoutAccountService.syncVerificationStatus(accountId);

        return ResponseEntity.noContent().build();
    }
}

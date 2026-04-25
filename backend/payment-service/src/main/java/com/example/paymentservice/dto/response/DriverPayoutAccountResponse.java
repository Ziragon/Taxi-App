package com.example.paymentservice.dto.response;

import com.example.paymentservice.entity.DriverPayoutAccount;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Счёт для выплат водителю")
public record DriverPayoutAccountResponse(

        @Schema(description = "ID счёта", example = "1")
        Long id,

        @Schema(description = "ID водителя", example = "789")
        Long driverId,

        @Schema(description = "Последние 4 цифры", example = "4242")
        String lastFour,

        @Schema(description = "Верифицирован", example = "true")
        boolean isVerified,

        @Schema(description = "Является дефолтным", example = "true")
        boolean isDefault,

        @Schema(description = "Дата добавления")
        Instant createdAt
) {
    public static DriverPayoutAccountResponse from(DriverPayoutAccount account) {
        return new DriverPayoutAccountResponse(
                account.getId(),
                account.getDriverId(),
                account.getLastFour(),
                account.isVerified(),
                account.isDefaultvalue(),
                account.getCreatedAt()
        );
    }
}

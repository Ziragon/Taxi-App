package com.example.paymentservice.dto.response;

import com.example.paymentservice.entity.PaymentMethod;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Платёжный метод")
public record PaymentMethodResponse(

        @Schema(description = "ID метода", example = "1")
        Long id,

        @Schema(description = "ID пассажира", example = "456")
        Long passengerId,

        @Schema(description = "Бренд карты", example = "Visa")
        String cardBrand,

        @Schema(description = "Последние 4 цифры", example = "4242")
        String lastFour,

        @Schema(description = "Является дефолтным", example = "true")
        boolean isDefault,

        @Schema(description = "Активен", example = "true")
        boolean isActive,

        @Schema(description = "Дата добавления")
        Instant createdAt
) {
    public static PaymentMethodResponse from(PaymentMethod paymentMethod) {
        return new PaymentMethodResponse(
                paymentMethod.getId(),
                paymentMethod.getPassengerId(),
                paymentMethod.getCardBrand(),
                paymentMethod.getLastFour(),
                paymentMethod.isDefaultvalue(),
                paymentMethod.isActive(),
                paymentMethod.getCreatedAt()
        );
    }
}

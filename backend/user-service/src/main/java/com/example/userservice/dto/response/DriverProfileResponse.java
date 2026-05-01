package com.example.userservice.dto.response;

import com.example.userservice.dto.data.DriverProfileDto;
import com.example.shared.dto.enums.DriverStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Профиль водителя")
public record DriverProfileResponse (

    @Schema(description = "ID аккаунта", example = "456")
    Long accountId,

    @Schema(description = "Имя", example = "Сергей")
    String firstName,

    @Schema(description = "Фамилия", example = "Сидоров")
    String lastName,

    @Schema(description = "URL фото", example = "https://cults3d.com/en/3d-model/art/six-seven-meme-character?srsltid=AfmBOooc9X4Jb3lmH25XJC5dcAyY9E44M9HvZPvOJVCAIs7SFjeEx0v8")
    String photoUrl,

    @Schema(description = "Номер водительского удостоверения", example = "7712345678")
    String licenseNumber,

    @Schema(description = "Статус", example = "ONLINE")
    DriverStatus status,

    @Schema(description = "Средний рейтинг", example = "4.92")
    BigDecimal averageRating,

    @Schema(description = "Количество поездок", example = "312")
    Integer totalTrips,

    @Schema(description = "Верифицирован ли", example = "true")
    boolean isVerified
) {
    public static DriverProfileResponse from(DriverProfileDto profile) {
        return new DriverProfileResponse(
                profile.accountId(),
                profile.firstName(),
                profile.lastName(),
                profile.photoUrl(),
                profile.licenseNumber(),
                profile.status(),
                profile.averageRating(),
                profile.totalTrips(),
                profile.verified()
        );
    }
}

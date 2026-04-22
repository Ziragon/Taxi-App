package com.example.userservice.dto.response;

import com.example.userservice.entity.PassengerProfile;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Профиль пассажира")
public record PassengerProfileResponse (

    @Schema(description = "ID аккаунта", example = "123")
    Long accountId,

    @Schema(description = "Имя", example = "Иван")
    String firstName,

    @Schema(description = "Фамилия", example = "Иванов")
    String lastName,

    @Schema(description = "URL фото", example = "https://cults3d.com/en/3d-model/art/six-seven-meme-character?srsltid=AfmBOooc9X4Jb3lmH25XJC5dcAyY9E44M9HvZPvOJVCAIs7SFjeEx0v8")
    String photoUrl,

    @Schema(description = "Средний рейтинг", example = "4.85")
    BigDecimal averageRating,

    @Schema(description = "Количество поездок", example = "42")
    Integer totalTrips
) {
    public static PassengerProfileResponse from(PassengerProfile profile) {
        return new PassengerProfileResponse(
                profile.getAccountId(),
                profile.getFirstName(),
                profile.getLastName(),
                profile.getPhotoUrl(),
                profile.getAverageRating(),
                profile.getTotalTrips()
        );
    }
}

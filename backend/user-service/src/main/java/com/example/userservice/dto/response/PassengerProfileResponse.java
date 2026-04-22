package com.example.userservice.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Профиль пассажира")
public class PassengerProfileResponse {

    @Schema(description = "ID аккаунта", example = "123")
    private Long accountId;

    @Schema(description = "Имя", example = "Иван")
    private String firstName;

    @Schema(description = "Фамилия", example = "Иванов")
    private String lastName;

    @Schema(description = "URL фото", example = "https://cults3d.com/en/3d-model/art/six-seven-meme-character?srsltid=AfmBOooc9X4Jb3lmH25XJC5dcAyY9E44M9HvZPvOJVCAIs7SFjeEx0v8")
    private String photoUrl;

    @Schema(description = "Средний рейтинг", example = "4.85")
    private BigDecimal averageRating;

    @Schema(description = "Количество поездок", example = "42")
    private Integer totalTrips;
}

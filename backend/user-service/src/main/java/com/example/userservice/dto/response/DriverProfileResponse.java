package com.example.userservice.dto.response;

import com.example.userservice.entity.enums.DriverStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Профиль водителя")
public class DriverProfileResponse {

    @Schema(description = "ID аккаунта", example = "456")
    private Long accountId;

    @Schema(description = "Имя", example = "Сергей")
    private String firstName;

    @Schema(description = "Фамилия", example = "Сидоров")
    private String lastName;

    @Schema(description = "URL фото", example = "https://cults3d.com/en/3d-model/art/six-seven-meme-character?srsltid=AfmBOooc9X4Jb3lmH25XJC5dcAyY9E44M9HvZPvOJVCAIs7SFjeEx0v8")
    private String photoUrl;

    @Schema(description = "Номер водительского удостоверения", example = "7712345678")
    private String licenseNumber;

    @Schema(description = "Статус", example = "ONLINE")
    private DriverStatus status;

    @Schema(description = "Средний рейтинг", example = "4.92")
    private BigDecimal averageRating;

    @Schema(description = "Количество поездок", example = "312")
    private Integer totalTrips;

    @Schema(description = "Верифицирован ли", example = "true")
    private Boolean isVerified;
}

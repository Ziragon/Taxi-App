package com.example.userservice.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Обновление профиля водителя")
public class UpdateDriverProfileRequest {

    @Size(max = 100, message = "Максимум 100 символов")
    @Schema(description = "Имя", example = "Сергей")
    private String firstName;

    @Size(max = 100, message = "Максимум 100 символов")
    @Schema(description = "Фамилия", example = "Петров")
    private String lastName;

    @Size(max = 50, message = "Максимум 50 символов")
    @Schema(description = "Номер водительского удостоверения", example = "7799887766")
    private String licenseNumber;

    @Schema(description = "URL фото профиля", example = "https://cults3d.com/en/3d-model/art/six-seven-meme-character?srsltid=AfmBOooc9X4Jb3lmH25XJC5dcAyY9E44M9HvZPvOJVCAIs7SFjeEx0v8")
    private String photoUrl;
}
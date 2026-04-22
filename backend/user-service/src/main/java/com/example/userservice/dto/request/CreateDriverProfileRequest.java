package com.example.userservice.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Создание профиля водителя")
public class CreateDriverProfileRequest {

    @NotBlank(message = "Имя обязательно")
    @Size(max = 100, message = "Максимум 100 символов")
    @Schema(description = "Имя", example = "Сергей")
    private String firstName;

    @NotBlank(message = "Фамилия обязательна")
    @Size(max = 100, message = "Максимум 100 символов")
    @Schema(description = "Фамилия", example = "Сидоров")
    private String lastName;

    @NotBlank(message = "Номер водительского удостоверения обязателен")
    @Size(max = 50, message = "Максимум 50 символов")
    @Schema(description = "Номер водительского удостоверения", example = "7712345678")
    private String licenseNumber;

    @Schema(description = "URL фото профиля", example = "https://cults3d.com/en/3d-model/art/six-seven-meme-character?srsltid=AfmBOooc9X4Jb3lmH25XJC5dcAyY9E44M9HvZPvOJVCAIs7SFjeEx0v8")
    private String photoUrl;
}

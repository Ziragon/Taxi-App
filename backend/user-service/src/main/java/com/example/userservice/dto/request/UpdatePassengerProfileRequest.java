package com.example.userservice.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(description = "Обновление профиля пассажира")
public record UpdatePassengerProfileRequest (

    @Size(max = 100, message = "Максимум 100 символов")
    @Schema(description = "Имя", example = "Иван")
    String firstName,

    @Size(max = 100, message = "Максимум 100 символов")
    @Schema(description = "Фамилия", example = "Петров")
    String lastName,

    @Schema(description = "URL фото профиля", example = "https://cults3d.com/en/3d-model/art/six-seven-meme-character?srsltid=AfmBOooc9X4Jb3lmH25XJC5dcAyY9E44M9HvZPvOJVCAIs7SFjeEx0v8")
    String photoUrl
) {}

package com.example.userservice.dto.response;

import com.example.userservice.dto.data.AuthDto;
import com.example.userservice.dto.data.TokenDto;
import com.example.userservice.entity.enums.AccountRole;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Ответ с токенами")
public record AuthResponse (

        @Schema(description = "ID аккаунта", example = "123")
        Long id,

        @Schema(description = "Email пользователя", example = "user@example.com")
        String email,

        @Schema(description = "Внутренняя роль пользователя", example = "USER")
        AccountRole role,

        TokenDto accessTokenDto,

        TokenDto refreshTokenDto,

        PassengerProfileResponse passengerProfile,

        DriverProfileResponse driverProfile
) {
    public static AuthResponse from(AuthDto result) {
        return new AuthResponse(
                result.accountDto().id(),
                result.accountDto().email(),
                result.accountDto().role(),
                result.accessTokenDto(),
                result.refreshTokenDto(),
                result.passengerProfileDto() != null
                        ? PassengerProfileResponse.from(result.passengerProfileDto())
                        : null,
                result.driverProfileDto() != null
                        ? DriverProfileResponse.from(result.driverProfileDto())
                        : null
        );
    }
}

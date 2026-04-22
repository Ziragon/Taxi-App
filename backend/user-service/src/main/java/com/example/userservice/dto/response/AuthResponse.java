package com.example.userservice.dto.response;

import com.example.userservice.dto.data.AccountDto;
import com.example.userservice.dto.data.AuthResult;
import com.example.userservice.dto.data.TokenData;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Ответ с токенами")
public record AuthResponse (

        AccountDto accountDto,

        TokenData accessTokenData,

        TokenData refreshTokenData
) {
    public static AuthResponse from(AuthResult result) {
        return new AuthResponse(
                AccountDto.from(result.account()),
                result.accessTokenData(),
                result.refreshTokenData()
        );
    }
}

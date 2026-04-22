package com.example.userservice.dto.data;

import com.example.userservice.entity.Account;

public record AuthResult(
        Account account,
        TokenData accessTokenData,
        TokenData refreshTokenData
) {}

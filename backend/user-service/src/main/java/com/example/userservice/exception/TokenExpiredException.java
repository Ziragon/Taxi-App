package com.example.userservice.exception;

import com.example.shared.exception.common.AuthenticationException;

public class TokenExpiredException extends AuthenticationException {

    public TokenExpiredException() {
        super("Token has expired");
    }

    public TokenExpiredException(String tokenType) {
        super("%s token has expired".formatted(tokenType));
    }
}
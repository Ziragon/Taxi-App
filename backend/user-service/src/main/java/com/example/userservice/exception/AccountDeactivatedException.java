package com.example.userservice.exception;

import com.example.shared.exception.common.AuthorizationException;

public class AccountDeactivatedException extends AuthorizationException {

    public AccountDeactivatedException(Long accountId) {
        super("Account %d is deactivated".formatted(accountId));
    }
}
package com.example.userservice.exception;

import com.example.shared.exception.common.ResourceAlreadyExistsException;

public class AccountAlreadyExistsException extends ResourceAlreadyExistsException {

    public AccountAlreadyExistsException(String field, Object value) {
        super("Account", field, value);
    }
}

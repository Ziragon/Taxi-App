package com.example.userservice.exception;

import com.example.shared.exception.common.ResourceNotFoundException;

public class AccountNotFoundException extends ResourceNotFoundException {

    public AccountNotFoundException(Long id) {
        super("Account", id);
    }

    public AccountNotFoundException(String field, Object value) {
        super("Account", field, value);
    }
}

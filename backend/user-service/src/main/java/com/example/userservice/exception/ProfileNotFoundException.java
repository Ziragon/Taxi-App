package com.example.userservice.exception;

import com.example.shared.exception.common.ResourceNotFoundException;

public class ProfileNotFoundException extends ResourceNotFoundException {

    public ProfileNotFoundException(String profileType, Long accountId) {
        super("%s profile".formatted(profileType), "accountId", accountId);
    }
}
package com.example.shared.security;

public record UserPrincipal(

        Long userId,

        String role
) {}

package com.example.userservice.dto.data.admin;

import com.example.userservice.entity.Account;
import com.example.userservice.entity.enums.AccountRole;

import java.time.Instant;

public record AccountAdminDto(
        Long id,
        String email,
        String phone,
        AccountRole role,
        boolean active,
        Instant createdAt
) {
    public static AccountAdminDto from(Account account) {
        return new AccountAdminDto(
                account.getId(),
                account.getEmail(),
                account.getPhone(),
                account.getRole(),
                account.isActive(),
                account.getCreatedAt()
        );
    }
}

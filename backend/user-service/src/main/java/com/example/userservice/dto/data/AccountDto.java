package com.example.userservice.dto.data;

import com.example.userservice.entity.Account;
import com.example.userservice.entity.enums.AccountRole;

public record AccountDto (
        Long id,

        String email,

        AccountRole role
) {
    public static AccountDto from(Account account) {
        return new AccountDto(
                account.getId(),
                account.getEmail(),
                account.getRole()
        );
    }
}

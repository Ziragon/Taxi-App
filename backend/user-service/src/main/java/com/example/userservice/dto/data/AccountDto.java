package com.example.userservice.dto.data;

import com.example.userservice.entity.Account;
import com.example.userservice.entity.enums.AccountRole;
import io.swagger.v3.oas.annotations.media.Schema;

public record AccountDto (
        @Schema(description = "ID аккаунта", example = "123")
        Long id,

        @Schema(description = "Email пользователя", example = "user@example.com")
        String email,

        @Schema(description = "Внутренняя роль пользователя", example = "USER")
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

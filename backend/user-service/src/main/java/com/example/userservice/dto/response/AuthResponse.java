package com.example.userservice.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Ответ с токенами")
public record AuthResponse (

    @Schema(description = "Access токен (JWT)", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c")
    String accessToken,

    @Schema(description = "Refresh токен (JWT)", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwiaWF0IjoxNTE2MjM5MDIyfQ.4Adcj0vbR-pXRlFkRHRwPq8J7fF6q5xjKq5M2qJ_8zU")
    String refreshToken
) {}

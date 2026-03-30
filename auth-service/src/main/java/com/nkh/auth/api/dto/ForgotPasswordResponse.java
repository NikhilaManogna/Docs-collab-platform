package com.nkh.auth.api.dto;

import java.time.Instant;

public record ForgotPasswordResponse(
        String message,
        Instant expiresAt,
        String inboxUrl
) {
}

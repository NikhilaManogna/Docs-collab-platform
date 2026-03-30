package com.nkh.auth.api.dto;

import java.util.UUID;

public record AuthResponse(
        UUID userId,
        String username,
        String role,
        String accessToken
) {
}

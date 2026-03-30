package com.nkh.auth.api.dto;

import java.util.UUID;

public record UserSummaryResponse(
        UUID userId,
        String username,
        String email
) {
}

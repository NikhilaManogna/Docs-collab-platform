package com.nkh.document.api.dto;

import com.nkh.document.domain.DocumentRole;

import java.time.Instant;
import java.util.UUID;

public record DocumentResponse(
        UUID id,
        String title,
        String content,
        UUID ownerId,
        long currentVersion,
        DocumentRole role,
        Instant createdAt,
        Instant updatedAt
) {
}

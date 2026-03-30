package com.nkh.document.api.dto;

import com.nkh.document.domain.DocumentRole;

import java.time.Instant;
import java.util.UUID;

public record DocumentSummaryResponse(
        UUID id,
        String title,
        long currentVersion,
        DocumentRole role,
        Instant updatedAt
) {
}

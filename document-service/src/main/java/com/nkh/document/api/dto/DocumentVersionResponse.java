package com.nkh.document.api.dto;

import com.nkh.document.domain.VersionEventType;

import java.time.Instant;
import java.util.UUID;

public record DocumentVersionResponse(
        Long id,
        long versionNumber,
        String title,
        String content,
        UUID createdBy,
        VersionEventType eventType,
        Instant createdAt
) {
}

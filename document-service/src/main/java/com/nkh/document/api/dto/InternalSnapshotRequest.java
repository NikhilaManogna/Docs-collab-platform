package com.nkh.document.api.dto;

import com.nkh.document.domain.VersionEventType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record InternalSnapshotRequest(
        @NotBlank @Size(max = 150) String title,
        @NotNull String content,
        @NotNull UUID updatedBy,
        @NotNull VersionEventType eventType
) {
}

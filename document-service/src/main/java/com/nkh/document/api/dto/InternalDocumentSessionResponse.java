package com.nkh.document.api.dto;

import com.nkh.document.domain.DocumentRole;

import java.util.UUID;

public record InternalDocumentSessionResponse(
        UUID documentId,
        String title,
        String content,
        long currentVersion,
        DocumentRole role
) {
}

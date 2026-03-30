package com.nkh.document.api.dto;

import com.nkh.document.domain.DocumentRole;

import java.util.UUID;

public record MembershipResponse(
        UUID userId,
        DocumentRole role
) {
}

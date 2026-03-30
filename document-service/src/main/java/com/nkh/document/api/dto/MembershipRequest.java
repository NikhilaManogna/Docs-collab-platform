package com.nkh.document.api.dto;

import com.nkh.document.domain.DocumentRole;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record MembershipRequest(
        @NotNull UUID userId,
        @NotNull DocumentRole role
) {
}

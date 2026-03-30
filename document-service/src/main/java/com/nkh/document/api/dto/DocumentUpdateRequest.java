package com.nkh.document.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record DocumentUpdateRequest(
        @NotBlank @Size(max = 150) String title,
        @NotNull String content
) {
}

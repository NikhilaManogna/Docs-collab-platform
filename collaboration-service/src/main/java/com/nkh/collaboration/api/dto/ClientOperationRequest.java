package com.nkh.collaboration.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ClientOperationRequest(
        @NotNull OperationType type,
        @Min(0) int index,
        String value,
        Integer length,
        String requestId
) {
}

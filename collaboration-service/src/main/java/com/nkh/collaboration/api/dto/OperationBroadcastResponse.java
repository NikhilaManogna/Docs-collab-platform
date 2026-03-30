package com.nkh.collaboration.api.dto;

import java.time.Instant;
import java.util.UUID;

public record OperationBroadcastResponse(
        UUID documentId,
        long version,
        OperationType type,
        int index,
        String value,
        Integer length,
        String actorId,
        String actorUsername,
        String content,
        String requestId,
        Instant occurredAt
) {
}

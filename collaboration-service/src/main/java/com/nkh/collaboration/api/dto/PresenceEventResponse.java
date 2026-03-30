package com.nkh.collaboration.api.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PresenceEventResponse(
        UUID documentId,
        String action,
        List<PresenceUser> activeUsers,
        Instant occurredAt
) {
}

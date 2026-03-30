package com.nkh.collaboration.redis;

import com.nkh.collaboration.api.dto.PresenceUser;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record DistributedPresenceEvent(
        String sourceInstance,
        UUID documentId,
        String action,
        List<PresenceUser> activeUsers,
        Instant occurredAt
) {
}

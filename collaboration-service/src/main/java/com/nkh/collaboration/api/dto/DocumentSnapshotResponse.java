package com.nkh.collaboration.api.dto;

import java.util.List;
import java.util.UUID;

public record DocumentSnapshotResponse(
        UUID documentId,
        String title,
        String content,
        long version,
        String role,
        List<PresenceUser> activeUsers
) {
}

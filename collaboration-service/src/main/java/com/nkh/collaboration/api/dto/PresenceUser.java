package com.nkh.collaboration.api.dto;

import java.util.UUID;

public record PresenceUser(
        UUID userId,
        String username
) {
}

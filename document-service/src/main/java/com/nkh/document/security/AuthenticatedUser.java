package com.nkh.document.security;

import java.util.Set;
import java.util.UUID;

public record AuthenticatedUser(
        UUID userId,
        String username,
        Set<String> roles
) {
}

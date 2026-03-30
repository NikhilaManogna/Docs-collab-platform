package com.nkh.collaboration.security;

import java.security.Principal;
import java.util.Set;
import java.util.UUID;

public record AuthenticatedSocketUser(
        UUID userId,
        String username,
        Set<String> roles
) implements Principal {

    @Override
    public String getName() {
        return userId.toString();
    }
}

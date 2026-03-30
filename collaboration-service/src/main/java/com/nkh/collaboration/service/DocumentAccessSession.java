package com.nkh.collaboration.service;

import java.util.UUID;

public record DocumentAccessSession(
        UUID documentId,
        String title,
        String content,
        long currentVersion,
        String role
) {
}

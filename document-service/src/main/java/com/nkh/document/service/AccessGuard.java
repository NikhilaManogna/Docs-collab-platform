package com.nkh.document.service;

import com.nkh.document.domain.DocumentMembershipEntity;
import com.nkh.document.domain.DocumentMembershipId;
import com.nkh.document.domain.DocumentRole;
import com.nkh.document.exception.ForbiddenException;
import com.nkh.document.exception.UnauthorizedException;
import com.nkh.document.repository.DocumentMembershipRepository;
import com.nkh.document.security.AuthenticatedUser;
import com.nkh.document.security.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class AccessGuard {

    private final DocumentMembershipRepository membershipRepository;

    public AccessGuard(DocumentMembershipRepository membershipRepository) {
        this.membershipRepository = membershipRepository;
    }

    public AuthenticatedUser requireUser() {
        AuthenticatedUser user = SecurityContextHolder.get();
        if (user == null) {
            throw new UnauthorizedException("Authentication is required");
        }
        return user;
    }

    public DocumentRole requireRole(UUID documentId, boolean editRequired) {
        AuthenticatedUser user = requireUser();
        DocumentMembershipEntity membership = membershipRepository.findById(new DocumentMembershipId(documentId, user.userId()))
                .orElseThrow(() -> new ForbiddenException("User does not have access to this document"));
        if (editRequired && !membership.getRole().canEdit()) {
            throw new ForbiddenException("User does not have edit permission");
        }
        return membership.getRole();
    }
}

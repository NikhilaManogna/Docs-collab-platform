package com.nkh.collaboration.api;

import com.nkh.collaboration.api.dto.DocumentSnapshotResponse;
import com.nkh.collaboration.security.AuthenticatedSocketUser;
import com.nkh.collaboration.security.JwtService;
import com.nkh.collaboration.service.CollaborationService;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/collaboration")
public class CollaborationController {

    private final CollaborationService collaborationService;
    private final JwtService jwtService;

    public CollaborationController(CollaborationService collaborationService, JwtService jwtService) {
        this.collaborationService = collaborationService;
        this.jwtService = jwtService;
    }

    @GetMapping("/documents/{documentId}/state")
    public DocumentSnapshotResponse state(
            @PathVariable("documentId") UUID documentId,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        AuthenticatedSocketUser user = jwtService.parse(authorization.substring(7));
        return collaborationService.currentState(documentId, user.userId());
    }
}

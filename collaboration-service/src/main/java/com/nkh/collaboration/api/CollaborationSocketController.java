package com.nkh.collaboration.api;

import com.nkh.collaboration.api.dto.ClientOperationRequest;
import com.nkh.collaboration.api.dto.DocumentSnapshotResponse;
import com.nkh.collaboration.api.dto.OperationBroadcastResponse;
import com.nkh.collaboration.security.AuthenticatedSocketUser;
import com.nkh.collaboration.security.AuthTokenHandshakeInterceptor;
import com.nkh.collaboration.security.JwtService;
import com.nkh.collaboration.service.CollaborationService;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.UUID;

@Controller
public class CollaborationSocketController {

    private final CollaborationService collaborationService;
    private final JwtService jwtService;

    public CollaborationSocketController(CollaborationService collaborationService, JwtService jwtService) {
        this.collaborationService = collaborationService;
        this.jwtService = jwtService;
    }

    @MessageMapping("/documents/{documentId}/join")
    public DocumentSnapshotResponse join(
            @DestinationVariable UUID documentId,
            Principal principal,
            SimpMessageHeaderAccessor accessor) {
        return collaborationService.join(documentId, user(principal, accessor), accessor.getSessionId());
    }

    @MessageMapping("/documents/{documentId}/operations")
    public OperationBroadcastResponse operate(
            @DestinationVariable UUID documentId,
            @Payload ClientOperationRequest request,
            Principal principal,
            SimpMessageHeaderAccessor accessor) {
        return collaborationService.processOperation(documentId, user(principal, accessor), request);
    }

    private AuthenticatedSocketUser user(Principal principal, SimpMessageHeaderAccessor accessor) {
        if (principal instanceof AuthenticatedSocketUser user) {
            return user;
        }
        if (accessor.getUser() instanceof AuthenticatedSocketUser user) {
            return user;
        }
        if (accessor.getSessionAttributes() != null) {
            Object token = accessor.getSessionAttributes().get(AuthTokenHandshakeInterceptor.ACCESS_TOKEN_ATTRIBUTE);
            if (token instanceof String value && !value.isBlank()) {
                return jwtService.parse(value);
            }
        }
        throw new IllegalStateException("WebSocket user is not authenticated");
    }
}

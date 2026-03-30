package com.nkh.collaboration.service;

import com.nkh.collaboration.api.dto.OperationType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;
import java.util.UUID;

@Component
public class DocumentClient {

    private final WebClient webClient;
    private final String internalServiceToken;

    public DocumentClient(
            WebClient.Builder builder,
            @Value("${app.services.document-base-url}") String documentBaseUrl,
            @Value("${app.security.internal-service-token}") String internalServiceToken) {
        this.webClient = builder.baseUrl(documentBaseUrl).build();
        this.internalServiceToken = internalServiceToken;
    }

    public DocumentAccessSession loadSession(UUID documentId, UUID userId) {
        InternalDocumentSessionResponse response = webClient.get()
                .uri(uriBuilder -> uriBuilder.path("/internal/documents/{id}/session").queryParam("userId", userId).build(documentId))
                .retrieve()
                .bodyToMono(InternalDocumentSessionResponse.class)
                .block();
        if (response == null) {
            throw new IllegalStateException("Document service returned an empty session");
        }
        return new DocumentAccessSession(
                response.documentId(),
                response.title(),
                response.content(),
                response.currentVersion(),
                response.role());
    }

    public void syncSnapshot(UUID documentId, UUID userId, String title, String content) {
        webClient.post()
                .uri("/internal/documents/{id}/snapshot", documentId)
                .header("X-Service-Token", internalServiceToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of(
                        "title", title,
                        "content", content,
                        "updatedBy", userId,
                        "eventType", "COLLAB_SYNC"))
                .retrieve()
                .toBodilessEntity()
                .block();
    }

    private record InternalDocumentSessionResponse(
            UUID documentId,
            String title,
            String content,
            long currentVersion,
            String role
    ) {
    }
}

package com.nkh.document.api;

import com.nkh.document.api.dto.DocumentResponse;
import com.nkh.document.api.dto.InternalDocumentSessionResponse;
import com.nkh.document.api.dto.InternalSnapshotRequest;
import com.nkh.document.service.DocumentService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/internal/documents")
public class InternalDocumentController {

    private final DocumentService documentService;

    public InternalDocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @GetMapping("/{id}/session")
    public InternalDocumentSessionResponse session(@PathVariable("id") UUID id, @RequestParam("userId") UUID userId) {
        return documentService.session(id, userId);
    }

    @PostMapping("/{id}/snapshot")
    public DocumentResponse snapshot(
            @PathVariable("id") UUID id,
            @RequestHeader("X-Service-Token") String serviceToken,
            @Valid @RequestBody InternalSnapshotRequest request) {
        return documentService.syncFromCollaboration(id, serviceToken, request);
    }
}

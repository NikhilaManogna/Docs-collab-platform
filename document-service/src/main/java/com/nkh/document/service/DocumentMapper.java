package com.nkh.document.service;

import com.nkh.document.api.dto.DocumentResponse;
import com.nkh.document.api.dto.DocumentSummaryResponse;
import com.nkh.document.api.dto.DocumentVersionResponse;
import com.nkh.document.api.dto.InternalDocumentSessionResponse;
import com.nkh.document.api.dto.MembershipResponse;
import com.nkh.document.domain.DocumentEntity;
import com.nkh.document.domain.DocumentMembershipEntity;
import com.nkh.document.domain.DocumentRole;
import com.nkh.document.domain.DocumentVersionEntity;
import org.springframework.stereotype.Component;

@Component
public class DocumentMapper {

    public DocumentResponse toResponse(DocumentEntity document, DocumentRole role) {
        return new DocumentResponse(
                document.getId(),
                document.getTitle(),
                document.getContent(),
                document.getOwnerId(),
                document.getCurrentVersion(),
                role,
                document.getCreatedAt(),
                document.getUpdatedAt());
    }

    public DocumentSummaryResponse toSummary(DocumentEntity document, DocumentRole role) {
        return new DocumentSummaryResponse(
                document.getId(),
                document.getTitle(),
                document.getCurrentVersion(),
                role,
                document.getUpdatedAt());
    }

    public DocumentVersionResponse toVersion(DocumentVersionEntity version) {
        return new DocumentVersionResponse(
                version.getId(),
                version.getVersionNumber(),
                version.getTitle(),
                version.getContent(),
                version.getCreatedBy(),
                version.getEventType(),
                version.getCreatedAt());
    }

    public MembershipResponse toMembership(DocumentMembershipEntity membership) {
        return new MembershipResponse(membership.getId().getUserId(), membership.getRole());
    }

    public InternalDocumentSessionResponse toSession(DocumentEntity document, DocumentRole role) {
        return new InternalDocumentSessionResponse(
                document.getId(),
                document.getTitle(),
                document.getContent(),
                document.getCurrentVersion(),
                role);
    }
}

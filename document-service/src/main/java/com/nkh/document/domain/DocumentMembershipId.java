package com.nkh.document.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class DocumentMembershipId implements Serializable {

    @Column(name = "document_id", nullable = false)
    private UUID documentId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    public DocumentMembershipId() {
    }

    public DocumentMembershipId(UUID documentId, UUID userId) {
        this.documentId = documentId;
        this.userId = userId;
    }

    public UUID getDocumentId() {
        return documentId;
    }

    public UUID getUserId() {
        return userId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof DocumentMembershipId that)) {
            return false;
        }
        return Objects.equals(documentId, that.documentId) && Objects.equals(userId, that.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(documentId, userId);
    }
}

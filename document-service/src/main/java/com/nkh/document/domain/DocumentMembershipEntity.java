package com.nkh.document.domain;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "document_memberships")
public class DocumentMembershipEntity {

    @EmbeddedId
    private DocumentMembershipId id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DocumentRole role;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    public DocumentMembershipId getId() {
        return id;
    }

    public void setId(DocumentMembershipId id) {
        this.id = id;
    }

    public DocumentRole getRole() {
        return role;
    }

    public void setRole(DocumentRole role) {
        this.role = role;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}

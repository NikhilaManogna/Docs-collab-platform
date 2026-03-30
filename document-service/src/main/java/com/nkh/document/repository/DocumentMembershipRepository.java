package com.nkh.document.repository;

import com.nkh.document.domain.DocumentMembershipEntity;
import com.nkh.document.domain.DocumentMembershipId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DocumentMembershipRepository extends JpaRepository<DocumentMembershipEntity, DocumentMembershipId> {

    List<DocumentMembershipEntity> findAllByIdUserId(UUID userId);

    List<DocumentMembershipEntity> findAllByIdDocumentId(UUID documentId);

    Optional<DocumentMembershipEntity> findByIdDocumentIdAndIdUserId(UUID documentId, UUID userId);
}

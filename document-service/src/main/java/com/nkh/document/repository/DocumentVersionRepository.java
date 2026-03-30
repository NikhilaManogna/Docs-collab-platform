package com.nkh.document.repository;

import com.nkh.document.domain.DocumentVersionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DocumentVersionRepository extends JpaRepository<DocumentVersionEntity, Long> {

    List<DocumentVersionEntity> findAllByDocumentIdOrderByVersionNumberDesc(UUID documentId);

    Optional<DocumentVersionEntity> findByDocumentIdAndId(UUID documentId, Long id);
}

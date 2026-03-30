package com.nkh.document.service;

import com.nkh.document.api.dto.DocumentCreateRequest;
import com.nkh.document.api.dto.DocumentResponse;
import com.nkh.document.api.dto.DocumentSummaryResponse;
import com.nkh.document.api.dto.DocumentUpdateRequest;
import com.nkh.document.api.dto.DocumentVersionResponse;
import com.nkh.document.api.dto.InternalDocumentSessionResponse;
import com.nkh.document.api.dto.InternalSnapshotRequest;
import com.nkh.document.api.dto.MembershipRequest;
import com.nkh.document.api.dto.MembershipResponse;
import com.nkh.document.domain.DocumentEntity;
import com.nkh.document.domain.DocumentMembershipEntity;
import com.nkh.document.domain.DocumentMembershipId;
import com.nkh.document.domain.DocumentRole;
import com.nkh.document.domain.DocumentVersionEntity;
import com.nkh.document.domain.VersionEventType;
import com.nkh.document.exception.ForbiddenException;
import com.nkh.document.exception.NotFoundException;
import com.nkh.document.repository.DocumentMembershipRepository;
import com.nkh.document.repository.DocumentRepository;
import com.nkh.document.repository.DocumentVersionRepository;
import com.nkh.document.security.AuthenticatedUser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final DocumentMembershipRepository membershipRepository;
    private final DocumentVersionRepository versionRepository;
    private final AccessGuard accessGuard;
    private final DocumentMapper mapper;
    private final String internalServiceToken;

    public DocumentService(
            DocumentRepository documentRepository,
            DocumentMembershipRepository membershipRepository,
            DocumentVersionRepository versionRepository,
            AccessGuard accessGuard,
            DocumentMapper mapper,
            @Value("${app.security.internal-service-token}") String internalServiceToken) {
        this.documentRepository = documentRepository;
        this.membershipRepository = membershipRepository;
        this.versionRepository = versionRepository;
        this.accessGuard = accessGuard;
        this.mapper = mapper;
        this.internalServiceToken = internalServiceToken;
    }

    @Transactional
    public DocumentResponse create(DocumentCreateRequest request) {
        AuthenticatedUser user = accessGuard.requireUser();
        DocumentEntity document = new DocumentEntity();
        document.setTitle(request.title().trim());
        document.setContent(request.content());
        document.setOwnerId(user.userId());
        document.setCurrentVersion(1L);
        documentRepository.save(document);

        DocumentMembershipEntity membership = new DocumentMembershipEntity();
        membership.setId(new DocumentMembershipId(document.getId(), user.userId()));
        membership.setRole(DocumentRole.OWNER);
        membershipRepository.save(membership);

        createVersion(document, user.userId(), VersionEventType.CREATED);
        return mapper.toResponse(document, DocumentRole.OWNER);
    }

    @Transactional(readOnly = true)
    public List<DocumentSummaryResponse> listMine() {
        AuthenticatedUser user = accessGuard.requireUser();
        return membershipRepository.findAllByIdUserId(user.userId()).stream()
                .map(membership -> mapper.toSummary(getDocument(membership.getId().getDocumentId()), membership.getRole()))
                .sorted(Comparator.comparing(DocumentSummaryResponse::updatedAt).reversed())
                .toList();
    }

    @Transactional(readOnly = true)
    public DocumentResponse get(UUID id) {
        DocumentRole role = accessGuard.requireRole(id, false);
        return mapper.toResponse(getDocument(id), role);
    }

    @Transactional
    public DocumentResponse update(UUID id, DocumentUpdateRequest request) {
        AuthenticatedUser user = accessGuard.requireUser();
        DocumentRole role = accessGuard.requireRole(id, true);
        DocumentEntity document = getDocument(id);
        document.setTitle(request.title().trim());
        document.setContent(request.content());
        document.setCurrentVersion(document.getCurrentVersion() + 1);
        createVersion(document, user.userId(), VersionEventType.UPDATED);
        return mapper.toResponse(document, role);
    }

    @Transactional
    public void delete(UUID id) {
        AuthenticatedUser user = accessGuard.requireUser();
        DocumentEntity document = getDocument(id);
        if (!document.getOwnerId().equals(user.userId())) {
            throw new ForbiddenException("Only the owner can delete the document");
        }
        membershipRepository.deleteAll(membershipRepository.findAllByIdDocumentId(id));
        versionRepository.deleteAll(versionRepository.findAllByDocumentIdOrderByVersionNumberDesc(id));
        documentRepository.delete(document);
    }

    @Transactional
    public MembershipResponse grantAccess(UUID id, MembershipRequest request) {
        AuthenticatedUser user = accessGuard.requireUser();
        DocumentEntity document = getDocument(id);
        if (!document.getOwnerId().equals(user.userId())) {
            throw new ForbiddenException("Only the owner can change access");
        }
        DocumentMembershipEntity membership = membershipRepository.findByIdDocumentIdAndIdUserId(id, request.userId())
                .orElseGet(DocumentMembershipEntity::new);
        membership.setId(new DocumentMembershipId(id, request.userId()));
        membership.setRole(request.role());
        membershipRepository.save(membership);
        return mapper.toMembership(membership);
    }

    @Transactional(readOnly = true)
    public List<MembershipResponse> listMembers(UUID id) {
        accessGuard.requireRole(id, false);
        return membershipRepository.findAllByIdDocumentId(id).stream().map(mapper::toMembership).toList();
    }

    @Transactional(readOnly = true)
    public List<DocumentVersionResponse> listVersions(UUID id) {
        accessGuard.requireRole(id, false);
        return versionRepository.findAllByDocumentIdOrderByVersionNumberDesc(id).stream().map(mapper::toVersion).toList();
    }

    @Transactional
    public DocumentResponse rollback(UUID id, Long versionId) {
        AuthenticatedUser user = accessGuard.requireUser();
        DocumentRole role = accessGuard.requireRole(id, true);
        DocumentEntity document = getDocument(id);
        DocumentVersionEntity targetVersion = versionRepository.findByDocumentIdAndId(id, versionId)
                .orElseThrow(() -> new NotFoundException("Version not found"));
        document.setTitle(targetVersion.getTitle());
        document.setContent(targetVersion.getContent());
        document.setCurrentVersion(document.getCurrentVersion() + 1);
        createVersion(document, user.userId(), VersionEventType.ROLLED_BACK);
        return mapper.toResponse(document, role);
    }

    @Transactional(readOnly = true)
    public InternalDocumentSessionResponse session(UUID id, UUID userId) {
        DocumentRole role = membershipRepository.findByIdDocumentIdAndIdUserId(id, userId)
                .map(DocumentMembershipEntity::getRole)
                .orElseThrow(() -> new ForbiddenException("User cannot join collaboration session"));
        return mapper.toSession(getDocument(id), role);
    }

    @Transactional
    public DocumentResponse syncFromCollaboration(UUID id, String serviceToken, InternalSnapshotRequest request) {
        if (!internalServiceToken.equals(serviceToken)) {
            throw new ForbiddenException("Invalid internal service token");
        }
        DocumentEntity document = getDocument(id);
        document.setTitle(request.title().trim());
        document.setContent(request.content());
        document.setCurrentVersion(document.getCurrentVersion() + 1);
        createVersion(document, request.updatedBy(), request.eventType());
        DocumentRole role = membershipRepository.findByIdDocumentIdAndIdUserId(id, request.updatedBy())
                .map(DocumentMembershipEntity::getRole)
                .orElse(DocumentRole.EDITOR);
        return mapper.toResponse(document, role);
    }

    private DocumentEntity getDocument(UUID id) {
        return documentRepository.findById(id).orElseThrow(() -> new NotFoundException("Document not found"));
    }

    private void createVersion(DocumentEntity document, UUID createdBy, VersionEventType eventType) {
        DocumentVersionEntity version = new DocumentVersionEntity();
        version.setDocumentId(document.getId());
        version.setVersionNumber(document.getCurrentVersion());
        version.setTitle(document.getTitle());
        version.setContent(document.getContent());
        version.setCreatedBy(createdBy);
        version.setEventType(eventType);
        versionRepository.save(version);
    }
}

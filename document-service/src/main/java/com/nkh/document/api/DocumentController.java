package com.nkh.document.api;

import com.nkh.document.api.dto.DocumentCreateRequest;
import com.nkh.document.api.dto.DocumentResponse;
import com.nkh.document.api.dto.DocumentSummaryResponse;
import com.nkh.document.api.dto.DocumentUpdateRequest;
import com.nkh.document.api.dto.DocumentVersionResponse;
import com.nkh.document.api.dto.MembershipRequest;
import com.nkh.document.api.dto.MembershipResponse;
import com.nkh.document.service.DocumentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DocumentResponse create(@Valid @RequestBody DocumentCreateRequest request) {
        return documentService.create(request);
    }

    @GetMapping
    public List<DocumentSummaryResponse> listMine() {
        return documentService.listMine();
    }

    @GetMapping("/{id}")
    public DocumentResponse get(@PathVariable("id") UUID id) {
        return documentService.get(id);
    }

    @PutMapping("/{id}")
    public DocumentResponse update(@PathVariable("id") UUID id, @Valid @RequestBody DocumentUpdateRequest request) {
        return documentService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable("id") UUID id) {
        documentService.delete(id);
    }

    @PostMapping("/{id}/members")
    public MembershipResponse grant(@PathVariable("id") UUID id, @Valid @RequestBody MembershipRequest request) {
        return documentService.grantAccess(id, request);
    }

    @GetMapping("/{id}/members")
    public List<MembershipResponse> listMembers(@PathVariable("id") UUID id) {
        return documentService.listMembers(id);
    }

    @GetMapping("/{id}/versions")
    public List<DocumentVersionResponse> versions(@PathVariable("id") UUID id) {
        return documentService.listVersions(id);
    }

    @PostMapping("/{id}/rollback/{versionId}")
    public DocumentResponse rollback(@PathVariable("id") UUID id, @PathVariable("versionId") Long versionId) {
        return documentService.rollback(id, versionId);
    }
}

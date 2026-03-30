CREATE SCHEMA IF NOT EXISTS document;
SET search_path TO document;

CREATE TABLE documents (
    id UUID PRIMARY KEY,
    title VARCHAR(150) NOT NULL,
    content TEXT NOT NULL,
    owner_id UUID NOT NULL,
    current_version BIGINT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE document_memberships (
    document_id UUID NOT NULL,
    user_id UUID NOT NULL,
    role VARCHAR(20) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY (document_id, user_id),
    CONSTRAINT fk_membership_document FOREIGN KEY (document_id) REFERENCES documents (id) ON DELETE CASCADE
);

CREATE TABLE document_versions (
    id BIGSERIAL PRIMARY KEY,
    document_id UUID NOT NULL,
    version_number BIGINT NOT NULL,
    title VARCHAR(150) NOT NULL,
    content TEXT NOT NULL,
    created_by UUID NOT NULL,
    event_type VARCHAR(30) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_version_document FOREIGN KEY (document_id) REFERENCES documents (id) ON DELETE CASCADE
);

CREATE INDEX idx_document_memberships_user_id ON document_memberships(user_id);
CREATE INDEX idx_document_versions_document_id ON document_versions(document_id);

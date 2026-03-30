package com.nkh.document.domain;

public enum DocumentRole {
    OWNER,
    EDITOR,
    VIEWER;

    public boolean canEdit() {
        return this == OWNER || this == EDITOR;
    }
}

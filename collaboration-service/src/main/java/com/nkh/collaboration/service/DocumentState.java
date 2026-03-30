package com.nkh.collaboration.service;

import com.nkh.collaboration.crdt.RgaDocument;

public class DocumentState {

    private final RgaDocument document;
    private String title;
    private long version;

    public DocumentState(String title, String content, long version) {
        this.document = RgaDocument.fromText(content);
        this.title = title;
        this.version = version;
    }

    public RgaDocument document() {
        return document;
    }

    public String title() {
        return title;
    }

    public void title(String title) {
        this.title = title;
    }

    public long version() {
        return version;
    }

    public void version(long version) {
        this.version = version;
    }
}

package com.nkh.collaboration.crdt;

public class RgaNode {

    private final String id;
    private final String leftId;
    private final String value;
    private boolean deleted;

    public RgaNode(String id, String leftId, String value) {
        this.id = id;
        this.leftId = leftId;
        this.value = value;
    }

    public String getId() {
        return id;
    }

    public String getLeftId() {
        return leftId;
    }

    public String getValue() {
        return value;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }
}

package com.nkh.collaboration.crdt;

public record InsertAtom(
        String id,
        String leftId,
        String value
) {
}

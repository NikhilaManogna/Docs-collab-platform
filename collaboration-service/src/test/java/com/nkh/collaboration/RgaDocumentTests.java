package com.nkh.collaboration;

import com.nkh.collaboration.crdt.RgaDocument;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RgaDocumentTests {

    @Test
    void preservesAllCharactersAcrossConcurrentStyleOperations() {
        RgaDocument document = RgaDocument.fromText("Hi");
        document.insert(2, "!", "alice", 2);
        document.insert(2, "?", "bob", 3);
        document.delete(1, 1);
        assertEquals(3, document.text().length());
    }
}

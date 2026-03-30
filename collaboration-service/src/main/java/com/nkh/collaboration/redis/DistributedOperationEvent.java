package com.nkh.collaboration.redis;

import com.nkh.collaboration.api.dto.OperationType;
import com.nkh.collaboration.crdt.InsertAtom;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record DistributedOperationEvent(
        String sourceInstance,
        UUID documentId,
        long version,
        OperationType type,
        int index,
        String value,
        Integer length,
        String actorId,
        String actorUsername,
        String requestId,
        String title,
        String content,
        List<InsertAtom> insertAtoms,
        List<String> deletedIds,
        Instant occurredAt
) {
}

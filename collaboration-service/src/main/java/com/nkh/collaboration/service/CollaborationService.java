package com.nkh.collaboration.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nkh.collaboration.api.dto.ClientOperationRequest;
import com.nkh.collaboration.api.dto.DocumentSnapshotResponse;
import com.nkh.collaboration.api.dto.OperationBroadcastResponse;
import com.nkh.collaboration.api.dto.OperationType;
import com.nkh.collaboration.api.dto.PresenceEventResponse;
import com.nkh.collaboration.api.dto.PresenceUser;
import com.nkh.collaboration.crdt.InsertAtom;
import com.nkh.collaboration.redis.DistributedOperationEvent;
import com.nkh.collaboration.redis.DistributedPresenceEvent;
import com.nkh.collaboration.redis.RedisEventPublisher;
import com.nkh.collaboration.security.AuthenticatedSocketUser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class CollaborationService {

    private final Map<UUID, DocumentState> states = new ConcurrentHashMap<>();
    private final Map<UUID, ReentrantLock> locks = new ConcurrentHashMap<>();

    private final DocumentClient documentClient;
    private final PresenceService presenceService;
    private final RedisEventPublisher publisher;
    private final SimpMessagingTemplate messagingTemplate;
    private final org.springframework.data.redis.core.StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final String instanceId;

    public CollaborationService(
            DocumentClient documentClient,
            PresenceService presenceService,
            RedisEventPublisher publisher,
            SimpMessagingTemplate messagingTemplate,
            org.springframework.data.redis.core.StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            @Value("${app.instance-id}") String instanceId) {
        this.documentClient = documentClient;
        this.presenceService = presenceService;
        this.publisher = publisher;
        this.messagingTemplate = messagingTemplate;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.instanceId = instanceId;
    }

    public DocumentSnapshotResponse join(UUID documentId, AuthenticatedSocketUser user, String sessionId) {
        DocumentAccessSession access = documentClient.loadSession(documentId, user.userId());
        DocumentState state = states.compute(documentId, (id, current) -> refreshState(current, access));
        List<PresenceUser> activeUsers = presenceService.join(documentId, user, sessionId);
        DocumentSnapshotResponse snapshot = new DocumentSnapshotResponse(
                documentId,
                state.title(),
                state.document().text(),
                state.version(),
                access.role(),
                activeUsers);
        messagingTemplate.convertAndSendToUser(user.getName(), "/queue/documents." + documentId + ".snapshot", snapshot);
        return snapshot;
    }

    public OperationBroadcastResponse processOperation(UUID documentId, AuthenticatedSocketUser user, ClientOperationRequest request) {
        DocumentAccessSession access = documentClient.loadSession(documentId, user.userId());
        if ("VIEWER".equals(access.role())) {
            throw new IllegalArgumentException("Viewer role cannot modify a document");
        }

        ReentrantLock lock = locks.computeIfAbsent(documentId, ignored -> new ReentrantLock());
        DistributedOperationEvent event;
        lock.lock();
        try {
            DocumentState state = states.compute(documentId, (id, current) -> refreshState(current, access));
            validate(request, state.document().length());
            long version = state.version() + 1;
            List<InsertAtom> insertAtoms = List.of();
            List<String> deletedIds = List.of();

            if (request.type() == OperationType.REPLACE) {
                state = new DocumentState(state.title(), request.value(), version);
                states.put(documentId, state);
            } else if (request.type() == OperationType.INSERT) {
                insertAtoms = state.document().insert(request.index(), request.value(), user.userId().toString(), version);
            } else {
                deletedIds = state.document().delete(request.index(), request.length());
            }

            state.title(access.title());
            state.version(version);
            String content = state.document().text();
            cacheState(documentId, state.title(), content, version);
            documentClient.syncSnapshot(documentId, user.userId(), state.title(), content);
            event = new DistributedOperationEvent(
                    instanceId,
                    documentId,
                    version,
                    request.type(),
                    request.index(),
                    request.value(),
                    request.length(),
                    user.userId().toString(),
                    user.username(),
                    request.requestId(),
                    state.title(),
                    content,
                    insertAtoms,
                    deletedIds,
                    Instant.now());
        } finally {
            lock.unlock();
        }

        publisher.publishOperation(event);
        broadcastOperation(event);
        return toResponse(event);
    }

    public DocumentSnapshotResponse currentState(UUID documentId, UUID userId) {
        DocumentAccessSession access = documentClient.loadSession(documentId, userId);
        DocumentState state = states.compute(documentId, (id, current) -> refreshState(current, access));
        return new DocumentSnapshotResponse(
                documentId,
                state.title(),
                state.document().text(),
                state.version(),
                access.role(),
                presenceService.activeUsers(documentId));
    }

    public void applyRemoteOperation(DistributedOperationEvent event) {
        if (instanceId.equals(event.sourceInstance())) {
            return;
        }
        ReentrantLock lock = locks.computeIfAbsent(event.documentId(), ignored -> new ReentrantLock());
        lock.lock();
        try {
            DocumentState state = states.get(event.documentId());
            if (state == null || event.version() > state.version() + 1) {
                DocumentState rebuilt = new DocumentState(event.title(), event.content(), event.version());
                states.put(event.documentId(), rebuilt);
                cacheState(event.documentId(), event.title(), event.content(), event.version());
            } else if (event.version() > state.version()) {
            if (event.type() == OperationType.INSERT) {
                event.insertAtoms().forEach(state.document()::applyInsert);
            } else if (event.type() == OperationType.REPLACE) {
                states.put(event.documentId(), new DocumentState(event.title(), event.content(), event.version()));
            } else {
                state.document().applyDelete(event.deletedIds());
            }
            if (event.type() != OperationType.REPLACE) {
                state.title(event.title());
                state.version(event.version());
            }
            if (event.type() != OperationType.REPLACE && !state.document().text().equals(event.content())) {
                states.put(event.documentId(), new DocumentState(event.title(), event.content(), event.version()));
            }
            cacheState(event.documentId(), event.title(), event.content(), event.version());
            } else {
                return;
            }
        } finally {
            lock.unlock();
        }
        broadcastOperation(event);
    }

    public void broadcastPresence(DistributedPresenceEvent event) {
        messagingTemplate.convertAndSend(
                "/topic/documents." + event.documentId() + ".presence",
                new PresenceEventResponse(event.documentId(), event.action(), event.activeUsers(), event.occurredAt()));
    }

    private DocumentState refreshState(DocumentState current, DocumentAccessSession access) {
        if (current == null) {
            return hydrate(access);
        }
        if (current.version() < access.currentVersion()) {
            return hydrate(access);
        }
        if (!current.title().equals(access.title()) || !current.document().text().equals(access.content())) {
            return hydrate(access);
        }
        return current;
    }

    private DocumentState hydrate(DocumentAccessSession access) {
        String cache = redisTemplate.opsForValue().get(stateKey(access.documentId()));
        if (cache != null) {
            try {
                CachedState cached = objectMapper.readValue(cache, CachedState.class);
                return new DocumentState(cached.title(), cached.content(), cached.version());
            } catch (JsonProcessingException ignored) {
            }
        }
        cacheState(access.documentId(), access.title(), access.content(), access.currentVersion());
        return new DocumentState(access.title(), access.content(), access.currentVersion());
    }

    private void validate(ClientOperationRequest request, int length) {
        if (request.type() == OperationType.REPLACE) {
            if (request.value() == null) {
                throw new IllegalArgumentException("Replace operations require content");
            }
            return;
        }
        if (request.index() > length) {
            throw new IllegalArgumentException("Operation index is out of bounds");
        }
        if (request.type() == OperationType.INSERT && (request.value() == null || request.value().isEmpty())) {
            throw new IllegalArgumentException("Insert operations require a non-empty value");
        }
        if (request.type() == OperationType.DELETE && (request.length() == null || request.length() <= 0)) {
            throw new IllegalArgumentException("Delete operations require a positive length");
        }
    }

    private void broadcastOperation(DistributedOperationEvent event) {
        messagingTemplate.convertAndSend("/topic/documents." + event.documentId() + ".operations", toResponse(event));
    }

    private OperationBroadcastResponse toResponse(DistributedOperationEvent event) {
        return new OperationBroadcastResponse(
                event.documentId(),
                event.version(),
                event.type(),
                event.index(),
                event.value(),
                event.length(),
                event.actorId(),
                event.actorUsername(),
                event.content(),
                event.requestId(),
                event.occurredAt());
    }

    private void cacheState(UUID documentId, String title, String content, long version) {
        try {
            redisTemplate.opsForValue().set(stateKey(documentId), objectMapper.writeValueAsString(new CachedState(title, content, version)));
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to cache document state", ex);
        }
    }

    private String stateKey(UUID documentId) {
        return "state:" + documentId;
    }

    private record CachedState(String title, String content, long version) {
    }
}

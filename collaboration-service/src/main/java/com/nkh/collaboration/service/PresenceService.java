package com.nkh.collaboration.service;

import com.nkh.collaboration.api.dto.PresenceUser;
import com.nkh.collaboration.redis.DistributedPresenceEvent;
import com.nkh.collaboration.redis.RedisEventPublisher;
import com.nkh.collaboration.security.AuthenticatedSocketUser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PresenceService {

    private final StringRedisTemplate redisTemplate;
    private final RedisEventPublisher publisher;
    private final String instanceId;
    private final Map<String, PresenceSession> sessions = new ConcurrentHashMap<>();

    public PresenceService(
            StringRedisTemplate redisTemplate,
            RedisEventPublisher publisher,
            @Value("${app.instance-id}") String instanceId) {
        this.redisTemplate = redisTemplate;
        this.publisher = publisher;
        this.instanceId = instanceId;
    }

    public List<PresenceUser> join(UUID documentId, AuthenticatedSocketUser user, String sessionId) {
        sessions.put(sessionId, new PresenceSession(documentId, user.userId(), user.username()));
        redisTemplate.opsForHash().put(key(documentId), sessionId, user.userId() + "|" + user.username());
        List<PresenceUser> active = activeUsers(documentId);
        publisher.publishPresence(new DistributedPresenceEvent(instanceId, documentId, "JOIN", active, Instant.now()));
        return active;
    }

    public void leave(String sessionId) {
        PresenceSession session = sessions.remove(sessionId);
        if (session == null) {
            return;
        }
        redisTemplate.opsForHash().delete(key(session.documentId()), sessionId);
        publisher.publishPresence(new DistributedPresenceEvent(
                instanceId, session.documentId(), "LEAVE", activeUsers(session.documentId()), Instant.now()));
    }

    public List<PresenceUser> activeUsers(UUID documentId) {
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(key(documentId));
        Map<UUID, PresenceUser> unique = new LinkedHashMap<>();
        for (Object value : entries.values()) {
            String[] tokens = String.valueOf(value).split("\\|", 2);
            if (tokens.length == 2) {
                UUID userId = UUID.fromString(tokens[0]);
                unique.putIfAbsent(userId, new PresenceUser(userId, tokens[1]));
            }
        }
        return new ArrayList<>(unique.values());
    }

    private String key(UUID documentId) {
        return "presence:" + documentId;
    }

    private record PresenceSession(UUID documentId, UUID userId, String username) {
    }
}

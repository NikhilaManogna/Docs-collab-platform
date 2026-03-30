package com.nkh.collaboration.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nkh.collaboration.service.CollaborationService;
import org.springframework.stereotype.Component;

@Component
public class RedisEventSubscriber {

    private final ObjectMapper objectMapper;
    private final CollaborationService collaborationService;

    public RedisEventSubscriber(ObjectMapper objectMapper, CollaborationService collaborationService) {
        this.objectMapper = objectMapper;
        this.collaborationService = collaborationService;
    }

    public void onOperation(String message) throws Exception {
        collaborationService.applyRemoteOperation(objectMapper.readValue(message, DistributedOperationEvent.class));
    }

    public void onPresence(String message) throws Exception {
        collaborationService.broadcastPresence(objectMapper.readValue(message, DistributedPresenceEvent.class));
    }
}

package com.nkh.collaboration.redis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class RedisEventPublisher {

    private final StringRedisTemplate redisTemplate;
    private final ChannelTopic operationTopic;
    private final ChannelTopic presenceTopic;
    private final ObjectMapper objectMapper;

    public RedisEventPublisher(
            StringRedisTemplate redisTemplate,
            @Qualifier("operationTopic") ChannelTopic operationTopic,
            @Qualifier("presenceTopic") ChannelTopic presenceTopic,
            ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.operationTopic = operationTopic;
        this.presenceTopic = presenceTopic;
        this.objectMapper = objectMapper;
    }

    public void publishOperation(DistributedOperationEvent event) {
        publish(operationTopic.getTopic(), event);
    }

    public void publishPresence(DistributedPresenceEvent event) {
        publish(presenceTopic.getTopic(), event);
    }

    private void publish(String topic, Object event) {
        try {
            redisTemplate.convertAndSend(topic, objectMapper.writeValueAsString(event));
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to publish redis event", ex);
        }
    }
}

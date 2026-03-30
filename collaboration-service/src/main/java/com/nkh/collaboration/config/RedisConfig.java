package com.nkh.collaboration.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration
public class RedisConfig {

    @Bean
    StringRedisTemplate stringRedisTemplate(RedisConnectionFactory factory) {
        StringRedisTemplate template = new StringRedisTemplate();
        template.setConnectionFactory(factory);
        template.setDefaultSerializer(new StringRedisSerializer());
        return template;
    }

    @Bean
    ChannelTopic operationTopic() {
        return new ChannelTopic("collaboration.operations");
    }

    @Bean
    ChannelTopic presenceTopic() {
        return new ChannelTopic("collaboration.presence");
    }

    @Bean
    @ConditionalOnProperty(value = "app.redis.listener-enabled", havingValue = "true", matchIfMissing = true)
    RedisMessageListenerContainer redisContainer(
            RedisConnectionFactory factory,
            @Qualifier("operationListener") MessageListener operationListener,
            @Qualifier("presenceListener") MessageListener presenceListener,
            @Qualifier("operationTopic") ChannelTopic operationTopic,
            @Qualifier("presenceTopic") ChannelTopic presenceTopic) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(factory);
        container.addMessageListener(operationListener, operationTopic);
        container.addMessageListener(presenceListener, presenceTopic);
        return container;
    }

    @Bean(name = "operationListener")
    MessageListener operationListenerAdapter(com.nkh.collaboration.redis.RedisEventSubscriber subscriber) {
        return new MessageListenerAdapter(subscriber, "onOperation");
    }

    @Bean(name = "presenceListener")
    MessageListener presenceListenerAdapter(com.nkh.collaboration.redis.RedisEventSubscriber subscriber) {
        return new MessageListenerAdapter(subscriber, "onPresence");
    }
}

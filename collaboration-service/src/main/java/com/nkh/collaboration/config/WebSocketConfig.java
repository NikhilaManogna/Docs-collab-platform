package com.nkh.collaboration.config;

import com.nkh.collaboration.security.WebSocketAuthChannelInterceptor;
import com.nkh.collaboration.security.AuthTokenHandshakeInterceptor;
import com.nkh.collaboration.security.AuthenticatedHandshakeHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final WebSocketAuthChannelInterceptor authChannelInterceptor;
    private final AuthTokenHandshakeInterceptor authTokenHandshakeInterceptor;
    private final AuthenticatedHandshakeHandler authenticatedHandshakeHandler;

    public WebSocketConfig(
            WebSocketAuthChannelInterceptor authChannelInterceptor,
            AuthTokenHandshakeInterceptor authTokenHandshakeInterceptor,
            AuthenticatedHandshakeHandler authenticatedHandshakeHandler) {
        this.authChannelInterceptor = authChannelInterceptor;
        this.authTokenHandshakeInterceptor = authTokenHandshakeInterceptor;
        this.authenticatedHandshakeHandler = authenticatedHandshakeHandler;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic", "/queue");
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws/collaboration")
                .setHandshakeHandler(authenticatedHandshakeHandler)
                .addInterceptors(authTokenHandshakeInterceptor)
                .setAllowedOriginPatterns("*");
        registry.addEndpoint("/ws/collaboration")
                .setHandshakeHandler(authenticatedHandshakeHandler)
                .addInterceptors(authTokenHandshakeInterceptor)
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(authChannelInterceptor);
    }
}

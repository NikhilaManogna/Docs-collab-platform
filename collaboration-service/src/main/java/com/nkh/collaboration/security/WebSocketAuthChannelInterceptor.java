package com.nkh.collaboration.security;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.stereotype.Component;

@Component
public class WebSocketAuthChannelInterceptor implements ChannelInterceptor {

    private final JwtService jwtService;

    public WebSocketAuthChannelInterceptor(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
        if (accessor.getUser() == null) {
            String header = accessor.getFirstNativeHeader("Authorization");
            String token = null;
            if (header != null && header.startsWith("Bearer ")) {
                token = header.substring(7);
            } else if (accessor.getSessionAttributes() != null) {
                Object candidate = accessor.getSessionAttributes().get(AuthTokenHandshakeInterceptor.ACCESS_TOKEN_ATTRIBUTE);
                if (candidate instanceof String value && !value.isBlank()) {
                    token = value;
                }
            }

            if (token != null && !token.isBlank()) {
                accessor.setUser(jwtService.parse(token));
            }
        }
        return message;
    }
}

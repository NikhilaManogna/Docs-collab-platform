package com.nkh.collaboration.security;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

import java.security.Principal;
import java.util.Map;

@Component
public class AuthenticatedHandshakeHandler extends DefaultHandshakeHandler {

    private final JwtService jwtService;

    public AuthenticatedHandshakeHandler(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected Principal determineUser(
            ServerHttpRequest request,
            WebSocketHandler wsHandler,
            Map<String, Object> attributes) {
        Object token = attributes.get(AuthTokenHandshakeInterceptor.ACCESS_TOKEN_ATTRIBUTE);
        if (token instanceof String value && !value.isBlank()) {
            return jwtService.parse(value);
        }
        return super.determineUser(request, wsHandler, attributes);
    }
}

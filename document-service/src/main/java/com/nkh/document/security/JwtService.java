package com.nkh.document.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Service
public class JwtService {

    private final SecretKey key;

    public JwtService(@Value("${app.security.jwt-secret}") String secret) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public AuthenticatedUser parse(String token) {
        Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
        Object rawRoles = claims.get("roles");
        Set<String> roles = rawRoles instanceof java.util.List<?> list
                ? list.stream().map(String::valueOf).collect(java.util.stream.Collectors.toSet())
                : new HashSet<>();
        return new AuthenticatedUser(UUID.fromString(claims.getSubject()), claims.get("username", String.class), roles);
    }
}

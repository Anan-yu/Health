package com.rayk.health.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

@Service
public class JwtService {
    private final JwtProperties properties;
    private final SecretKey key;

    public JwtService(JwtProperties properties) {
        this.properties = properties;
        this.key = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
    }

    public IssuedToken issue(
            String username,
            long userId,
            long tenantId,
            List<String> roles,
            List<String> permissions) {
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(properties.expireSeconds());
        String jti = UUID.randomUUID().toString();
        String token =
                Jwts.builder()
                        .id(jti)
                        .subject(username)
                        .claim("userId", userId)
                        .claim("tenantId", tenantId)
                        .claim("roles", roles)
                        .claim("permissions", permissions)
                        .issuedAt(Date.from(now))
                        .expiration(Date.from(expiresAt))
                        .signWith(key)
                        .compact();
        return new IssuedToken(token, jti, properties.expireSeconds());
    }

    public Claims parse(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }

    public record IssuedToken(String token, String jti, long expiresIn) {}
}


package com.trackpredict.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

@Service
public class JwtService {
    private final SecretKey key;
    private final Duration ttl;

    public JwtService(@Value("${trackpredict.jwt.secret}") String secret,
                      @Value("${trackpredict.jwt.expiration-minutes}") long minutes) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.ttl = Duration.ofMinutes(minutes);
    }

    public String generate(AuthPrincipal p) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(p.email())
                .claim("uid", p.userId())
                .claim("emp", p.empresaId())
                .claim("rol", p.rol())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(ttl)))
                .signWith(key)
                .compact();
    }

    public AuthPrincipal parse(String token) {
        Claims c = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
        return new AuthPrincipal(((Number) c.get("uid")).longValue(), ((Number) c.get("emp")).longValue(),
                c.getSubject(), c.get("rol", String.class));
    }
}

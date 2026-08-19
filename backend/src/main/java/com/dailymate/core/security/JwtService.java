package com.dailymate.core.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {
    private final SecretKey key;
    private final Duration ttl;
    public JwtService(@Value("${app.security.jwt.secret}") String secret, @Value("${app.security.jwt.access-token-ttl}") Duration ttl) {
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        this.ttl = ttl;
    }
    public String issue(UserPrincipal principal) {
        Instant now = Instant.now();
        return Jwts.builder().subject(principal.user().getId()).claim("role", principal.user().getRole().name())
                .issuedAt(Date.from(now)).expiration(Date.from(now.plus(ttl))).signWith(key).compact();
    }
    public String subject(String token) { return claims(token).getSubject(); }
    public boolean isValid(String token, UserPrincipal principal) { return principal.user().getId().equals(subject(token)); }
    public long expiresInSeconds() { return ttl.toSeconds(); }
    private Claims claims(String token) { return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload(); }
}

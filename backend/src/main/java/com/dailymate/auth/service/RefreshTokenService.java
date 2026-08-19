package com.dailymate.auth.service;

import com.dailymate.auth.entity.RefreshToken;
import com.dailymate.auth.repository.RefreshTokenRepository;
import com.dailymate.core.exception.UnauthorizedException;
import com.dailymate.user.entity.User;
import com.dailymate.user.repository.UserRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RefreshTokenService {

    private final RefreshTokenRepository tokens;
    private final UserRepository users;
    private final Duration ttl;
    private final SecureRandom secureRandom = new SecureRandom();

    public RefreshTokenService(
            RefreshTokenRepository tokens,
            UserRepository users,
            @Value("${app.security.jwt.refresh-token-ttl}") Duration ttl) {
        this.tokens = tokens;
        this.users = users;
        this.ttl = ttl;
    }

    @Transactional
    public IssuedRefreshToken issue(User user) {
        String rawToken = generateToken();
        RefreshToken token = new RefreshToken();
        token.setUserId(user.getId());
        token.setTokenHash(hash(rawToken));
        token.setExpiresAt(Instant.now().plus(ttl));
        tokens.save(token);
        return new IssuedRefreshToken(rawToken, ttl.toSeconds());
    }

    @Transactional
    public User rotate(String rawToken) {
        RefreshToken token = tokens.findByTokenHash(hash(rawToken))
                .filter(RefreshToken::isActive)
                .orElseThrow(() -> new UnauthorizedException("Invalid or expired refresh token"));
        token.setRevokedAt(Instant.now());
        return users.findById(token.getUserId())
                .orElseThrow(() -> new UnauthorizedException("Invalid or expired refresh token"));
    }

    @Transactional
    public void revoke(String rawToken) {
        tokens.findByTokenHash(hash(rawToken)).ifPresent(token -> {
            if (token.getRevokedAt() == null) {
                token.setRevokedAt(Instant.now());
            }
        });
    }

    public long expiresInSeconds() {
        return ttl.toSeconds();
    }

    private String generateToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    static String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    public record IssuedRefreshToken(String token, long expiresInSeconds) {}
}

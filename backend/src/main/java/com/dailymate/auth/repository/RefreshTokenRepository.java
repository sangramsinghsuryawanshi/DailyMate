package com.dailymate.auth.repository;

import com.dailymate.auth.entity.RefreshToken;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, String> {
    Optional<RefreshToken> findByTokenHash(String tokenHash);

    @Modifying
    @Query("UPDATE RefreshToken r SET r.revokedAt = :revokedAt WHERE r.tokenHash = :tokenHash AND r.revokedAt IS NULL AND r.expiresAt > :now")
    int revokeIfActive(@Param("tokenHash") String tokenHash, @Param("revokedAt") Instant revokedAt, @Param("now") Instant now);
}

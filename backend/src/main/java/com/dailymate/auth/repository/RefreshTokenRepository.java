package com.dailymate.auth.repository;

import com.dailymate.auth.entity.RefreshToken;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, String> {
    Optional<RefreshToken> findByTokenHash(String tokenHash);
}

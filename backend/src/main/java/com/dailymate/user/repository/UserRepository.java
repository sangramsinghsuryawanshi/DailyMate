package com.dailymate.user.repository;

import com.dailymate.user.entity.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

import com.dailymate.user.entity.UserStatus;

public interface UserRepository extends JpaRepository<User, String> {
    Optional<User> findByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCase(String email);
    long countByStatus(UserStatus status);
}

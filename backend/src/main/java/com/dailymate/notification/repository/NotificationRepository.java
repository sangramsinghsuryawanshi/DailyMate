package com.dailymate.notification.repository;

import com.dailymate.notification.entity.Notification;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface NotificationRepository extends JpaRepository<Notification, String> {
    List<Notification> findByUserIdOrderByCreatedAtDesc(String userId);
    Page<Notification> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);
    Optional<Notification> findByIdAndUserId(String id, String userId);
}

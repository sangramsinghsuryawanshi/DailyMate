package com.dailymate.assistant.repository;

import com.dailymate.assistant.entity.AssistantAction;
import com.dailymate.assistant.entity.AssistantActionStatus;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AssistantActionRepository extends JpaRepository<AssistantAction, String> {
    Optional<AssistantAction> findByIdAndUserId(String id, String userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM AssistantAction a WHERE a.id = :id AND a.userId = :userId")
    Optional<AssistantAction> findByIdAndUserIdForUpdate(@Param("id") String id, @Param("userId") String userId);

    List<AssistantAction> findByUserIdOrderByCreatedAtDesc(String userId);

    @Modifying
    @Query("DELETE FROM AssistantAction a WHERE a.expiresAt < :cutoff AND a.status IN :statuses")
    int deleteByExpiresAtBeforeAndStatusIn(@Param("cutoff") Instant cutoff, @Param("statuses") Collection<AssistantActionStatus> statuses);

    @Modifying
    @Query("DELETE FROM AssistantAction a WHERE a.executedAt < :cutoff AND a.status = :status")
    int deleteByExecutedAtBeforeAndStatus(@Param("cutoff") Instant cutoff, @Param("status") AssistantActionStatus status);
}

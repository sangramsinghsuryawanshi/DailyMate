package com.dailymate.assistant.repository;

import com.dailymate.assistant.entity.AssistantBulkOperation;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AssistantBulkOperationRepository extends JpaRepository<AssistantBulkOperation, String> {

    Optional<AssistantBulkOperation> findByBulkExecutionId(String bulkExecutionId);

    Optional<AssistantBulkOperation> findByIdAndActorId(String id, String actorId);
}

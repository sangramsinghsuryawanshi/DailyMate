package com.dailymate.assistant.repository;

import com.dailymate.assistant.entity.AssistantConversation;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AssistantConversationRepository extends JpaRepository<AssistantConversation, String> {
    List<AssistantConversation> findByUserIdOrderByCreatedAtDesc(String userId);
    Optional<AssistantConversation> findByIdAndUserId(String id, String userId);
}

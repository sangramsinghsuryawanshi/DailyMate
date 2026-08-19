package com.dailymate.assistant.repository;

import com.dailymate.assistant.entity.AssistantConversation;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AssistantConversationRepository extends JpaRepository<AssistantConversation, String> {
    List<AssistantConversation> findByUserIdOrderByCreatedAtDesc(String userId);
}

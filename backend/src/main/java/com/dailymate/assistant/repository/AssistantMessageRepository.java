package com.dailymate.assistant.repository;

import com.dailymate.assistant.entity.AssistantMessage;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AssistantMessageRepository extends JpaRepository<AssistantMessage, String> {
    List<AssistantMessage> findByConversationIdAndUserIdOrderByCreatedAtAsc(String conversationId, String userId);
    void deleteByConversationIdAndUserId(String conversationId, String userId);
}

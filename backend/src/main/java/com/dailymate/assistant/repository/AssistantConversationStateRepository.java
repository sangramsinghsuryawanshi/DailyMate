package com.dailymate.assistant.repository;

import com.dailymate.assistant.entity.AssistantConversationState;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AssistantConversationStateRepository extends JpaRepository<AssistantConversationState, String> {

    Optional<AssistantConversationState> findByConversationIdAndUserId(String conversationId, String userId);

    Optional<AssistantConversationState> findByConversationId(String conversationId);

    void deleteByConversationIdAndUserId(String conversationId, String userId);
}

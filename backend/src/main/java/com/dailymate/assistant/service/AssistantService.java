package com.dailymate.assistant.service;

import com.dailymate.assistant.dto.request.AssistantConversationRequest;
import com.dailymate.assistant.dto.response.AssistantConversationResponse;
import com.dailymate.assistant.entity.AssistantConversation;
import com.dailymate.assistant.repository.AssistantConversationRepository;
import com.dailymate.core.exception.NotFoundException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AssistantService {

    private final AssistantConversationRepository conversations;

    public AssistantService(AssistantConversationRepository conversations) {
        this.conversations = conversations;
    }

    public List<AssistantConversationResponse> getConversations(String userId) {
        return conversations.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    public AssistantConversationResponse createConversation(String userId, AssistantConversationRequest request) {
        AssistantConversation conversation = new AssistantConversation();
        conversation.setUserId(userId);
        applyChanges(conversation, request);
        return toResponse(conversations.save(conversation));
    }

    @Transactional
    public AssistantConversationResponse updateConversation(String userId, String conversationId, AssistantConversationRequest request) {
        AssistantConversation conversation = findConversation(userId, conversationId);
        applyChanges(conversation, request);
        return toResponse(conversations.save(conversation));
    }

    @Transactional
    public void deleteConversation(String userId, String conversationId) {
        AssistantConversation conversation = findConversation(userId, conversationId);
        conversations.delete(conversation);
    }

    private void applyChanges(AssistantConversation conversation, AssistantConversationRequest request) {
        conversation.setTitle(request.title().trim());
        conversation.setPrompt(request.prompt().trim());
        conversation.setResponse(request.response().trim());
    }

    private AssistantConversation findConversation(String userId, String conversationId) {
        return conversations.findById(conversationId)
                .filter(item -> item.getUserId().equals(userId))
                .orElseThrow(() -> new NotFoundException("Conversation not found"));
    }

    private AssistantConversationResponse toResponse(AssistantConversation conversation) {
        return new AssistantConversationResponse(
                conversation.getId(),
                conversation.getUserId(),
                conversation.getTitle(),
                conversation.getPrompt(),
                conversation.getResponse(),
                conversation.getCreatedAt());
    }
}

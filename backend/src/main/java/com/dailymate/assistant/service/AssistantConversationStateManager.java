package com.dailymate.assistant.service;

import com.dailymate.assistant.entity.AssistantConversationState;
import com.dailymate.assistant.repository.AssistantConversationStateRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Manages conversation continuity, multi-turn entity resolution, and session state.
 * Invariants: Strict tenant isolation (userId + conversationId), 30-minute sliding TTL, zero domain repository bypass.
 */
@Service
public class AssistantConversationStateManager {

    private static final Logger log = LoggerFactory.getLogger("ASSISTANT_STATE");
    public static final Duration CONVERSATION_STATE_TTL = Duration.ofMinutes(30);

    private final AssistantConversationStateRepository stateRepo;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AssistantConversationStateManager(AssistantConversationStateRepository stateRepo) {
        this.stateRepo = stateRepo;
    }

    @Transactional
    public AssistantConversationState getOrCreateState(String userId, String conversationId) {
        if (conversationId == null || conversationId.isBlank()) {
            return null;
        }

        Optional<AssistantConversationState> existingOpt = stateRepo.findByConversationIdAndUserId(conversationId, userId);
        Instant now = Instant.now();

        if (existingOpt.isPresent()) {
            AssistantConversationState state = existingOpt.get();
            if (state.getExpiresAt() != null && now.isAfter(state.getExpiresAt())) {
                log.info("CONVERSATION_STATE_EXPIRED conversationId={} userId={}", conversationId, userId);
                state.setTurnCount(1);
                state.setActiveProposalId(null);
                state.setLastActionId(null);
                state.setLastEntityId(null);
                state.setLastEntityType(null);
                state.setLastEntitySummary(null);
                state.setToolName(null);
                state.setUnresolvedParamsJson(null);
                state.setUpdatedAt(now);
                state.setExpiresAt(now.plus(CONVERSATION_STATE_TTL));
                return stateRepo.save(state);
            } else {
                state.setTurnCount(state.getTurnCount() + 1);
                state.setUpdatedAt(now);
                state.setExpiresAt(now.plus(CONVERSATION_STATE_TTL));
                return stateRepo.save(state);
            }
        }

        AssistantConversationState newState = new AssistantConversationState();
        newState.setConversationId(conversationId);
        newState.setUserId(userId);
        newState.setTurnCount(1);
        newState.setUpdatedAt(now);
        newState.setExpiresAt(now.plus(CONVERSATION_STATE_TTL));
        return stateRepo.save(newState);
    }

    @Transactional
    public void recordProposedAction(String userId, String conversationId, String proposalId, String toolName, String entityType, String entityId, String summary) {
        if (conversationId == null) return;
        stateRepo.findByConversationIdAndUserId(conversationId, userId).ifPresent(state -> {
            state.setActiveProposalId(proposalId);
            state.setToolName(toolName);
            state.setLastEntityType(entityType);
            state.setLastEntityId(entityId);
            state.setLastEntitySummary(summary);
            state.setUpdatedAt(Instant.now());
            state.setExpiresAt(Instant.now().plus(CONVERSATION_STATE_TTL));
            stateRepo.save(state);
            log.info("CONVERSATION_PROPOSAL_RECORDED conversationId={} proposalId={} toolName={}", conversationId, proposalId, toolName);
        });
    }

    @Transactional
    public void recordExecutedAction(String userId, String conversationId, String actionId, String toolName, String entityType, String entityId, String summary) {
        if (conversationId == null) return;
        stateRepo.findByConversationIdAndUserId(conversationId, userId).ifPresent(state -> {
            state.setActiveProposalId(null);
            state.setLastActionId(actionId);
            state.setToolName(toolName);
            state.setLastEntityType(entityType);
            state.setLastEntityId(entityId);
            state.setLastEntitySummary(summary);
            state.setUnresolvedParamsJson(null);
            state.setUpdatedAt(Instant.now());
            state.setExpiresAt(Instant.now().plus(CONVERSATION_STATE_TTL));
            stateRepo.save(state);
            log.info("CONVERSATION_EXECUTED_RECORDED conversationId={} actionId={} entityId={}", conversationId, actionId, entityId);
        });
    }

    @Transactional
    public void recordUnresolvedParams(String userId, String conversationId, Map<String, Object> params) {
        if (conversationId == null) return;
        stateRepo.findByConversationIdAndUserId(conversationId, userId).ifPresent(state -> {
            try {
                state.setUnresolvedParamsJson(objectMapper.writeValueAsString(params));
                state.setUpdatedAt(Instant.now());
                state.setExpiresAt(Instant.now().plus(CONVERSATION_STATE_TTL));
                stateRepo.save(state);
            } catch (Exception ignored) {}
        });
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getUnresolvedParams(String userId, String conversationId) {
        if (conversationId == null) return Map.of();
        return stateRepo.findByConversationIdAndUserId(conversationId, userId)
                .map(state -> {
                    try {
                        if (state.getUnresolvedParamsJson() != null) {
                            return objectMapper.readValue(state.getUnresolvedParamsJson(), new TypeReference<Map<String, Object>>() {});
                        }
                    } catch (Exception ignored) {}
                    return new HashMap<String, Object>();
                })
                .orElse(Map.of());
    }

    @Transactional
    public void clearUnresolvedParams(String userId, String conversationId) {
        if (conversationId == null) return;
        stateRepo.findByConversationIdAndUserId(conversationId, userId).ifPresent(state -> {
            state.setUnresolvedParamsJson(null);
            stateRepo.save(state);
        });
    }
}

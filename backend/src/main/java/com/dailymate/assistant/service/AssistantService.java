package com.dailymate.assistant.service;

import com.dailymate.assistant.dto.AssistantContext;
import com.dailymate.assistant.dto.request.AssistantActionExecutionRequest;
import com.dailymate.assistant.dto.request.AssistantChatRequest;
import com.dailymate.assistant.dto.response.AssistantActionExecutionResponse;
import com.dailymate.assistant.dto.response.AssistantActionProposalResponse;
import com.dailymate.assistant.dto.response.AssistantChatResponse;
import com.dailymate.assistant.dto.response.AssistantConversationResponse;
import com.dailymate.assistant.dto.response.AssistantMessageResponse;
import com.dailymate.assistant.entity.AssistantConversation;
import com.dailymate.assistant.entity.AssistantMessage;
import com.dailymate.assistant.repository.AssistantConversationRepository;
import com.dailymate.assistant.repository.AssistantMessageRepository;
import com.dailymate.assistant.security.AssistantAuditLogger;
import com.dailymate.assistant.security.AssistantPromptSanitizer;
import com.dailymate.assistant.security.AssistantRateLimiter;
import com.dailymate.assistant.security.AssistantResponseRedactor;
import com.dailymate.core.exception.NotFoundException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AssistantService {

    private final AssistantConversationRepository conversations;
    private final AssistantMessageRepository messages;
    private final AssistantContextService contextService;
    private final AssistantGroundingEngine groundingEngine;
    private final AssistantActionService actionService;
    private final AssistantConversationStateManager stateManager;
    private final AssistantRateLimiter rateLimiter;
    private final AssistantPromptSanitizer promptSanitizer;
    private final AssistantResponseRedactor responseRedactor;
    private final AssistantAuditLogger auditLogger;
    private final ObjectMapper objectMapper;

    public AssistantService(
            AssistantConversationRepository conversations,
            AssistantMessageRepository messages,
            AssistantContextService contextService,
            AssistantGroundingEngine groundingEngine,
            AssistantActionService actionService,
            AssistantConversationStateManager stateManager,
            AssistantRateLimiter rateLimiter,
            AssistantPromptSanitizer promptSanitizer,
            AssistantResponseRedactor responseRedactor,
            AssistantAuditLogger auditLogger) {
        this.conversations = conversations;
        this.messages = messages;
        this.contextService = contextService;
        this.groundingEngine = groundingEngine;
        this.actionService = actionService;
        this.stateManager = stateManager;
        this.rateLimiter = rateLimiter;
        this.promptSanitizer = promptSanitizer;
        this.responseRedactor = responseRedactor;
        this.auditLogger = auditLogger;
        this.objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    }

    public List<AssistantConversationResponse> getConversations(String userId) {
        return conversations.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    public List<AssistantMessageResponse> getConversationMessages(String userId, String conversationId) {
        conversations.findByIdAndUserId(conversationId, userId)
                .orElseThrow(() -> new NotFoundException("Conversation not found"));

        return messages.findByConversationIdAndUserIdOrderByCreatedAtAsc(conversationId, userId).stream()
                .map(this::toMessageResponse)
                .toList();
    }

    @Transactional
    public AssistantChatResponse chat(String userId, AssistantChatRequest request) {
        String correlationId = UUID.randomUUID().toString();
        long startTime = System.currentTimeMillis();
        String status = "SUCCESS";

        try {
            // 1. Rate Limiting Check (Per-User)
            rateLimiter.checkLimit(userId);

            String prompt = request.prompt().trim();
            String rawResponse;
            AssistantActionProposalResponse proposal = null;

            // 2. Resolve / Initialize Conversation ID
            String conversationId = (request.conversationId() != null && !request.conversationId().isBlank())
                    ? request.conversationId()
                    : UUID.randomUUID().toString();
            stateManager.getOrCreateState(userId, conversationId);

            // 3. Prompt Security Check (Adversarial / Injection Rejection)
            if (promptSanitizer.isAdversarialOrRestricted(prompt)) {
                rawResponse = promptSanitizer.getSafeRejectionMessage();
            } else {
                // 4. Authorized Tenant Context Retrieval
                AssistantContext context = contextService.buildContext(userId);

                // 5. Grounding Engine with Multi-Turn Intent & State Resolution
                AssistantGroundingEngine.GroundingResult result = groundingEngine.process(prompt, userId, context, conversationId);
                rawResponse = result.textResponse();

                if (result.proposal() != null) {
                    proposal = actionService.createProposal(
                            userId,
                            result.proposal().actionType(),
                            result.proposal().summary(),
                            result.proposal().parametersJson());

                    var state = stateManager.getOrCreateState(userId, conversationId);
                    if (state != null && state.getActiveProposalId() != null && !state.getActiveProposalId().equals(proposal.actionId())) {
                        actionService.supersedeProposal(userId, state.getActiveProposalId());
                    }

                    String entityType = "EXPENSE";
                    if (result.proposal().actionType().contains("REMINDER") || result.proposal().actionType().contains("MEDICINE")) {
                        entityType = "MEDICINE";
                    } else if (result.proposal().actionType().contains("CONTACT") || result.proposal().actionType().contains("ICE")) {
                        entityType = "EMERGENCY_CONTACT";
                    } else if (result.proposal().actionType().contains("EVENT")) {
                        entityType = "EVENT";
                    } else if (result.proposal().actionType().contains("JOB")) {
                        entityType = "JOB";
                    }

                    stateManager.recordProposedAction(
                            userId,
                            conversationId,
                            proposal.actionId(),
                            result.proposal().actionType(),
                            entityType,
                            null,
                            result.proposal().summary());
                }
            }

            // 6. Response Safety & Redaction Layer (Absolute Boundary)
            String safeResponse = responseRedactor.redact(rawResponse);

            // 7. Persistence of Conversation Header (Preserves first title or creates new)
            String title = prompt.length() > 40 ? prompt.substring(0, 37) + "..." : prompt;

            AssistantConversation conversation;
            if (request.conversationId() != null && !request.conversationId().isBlank()) {
                conversation = conversations.findByIdAndUserId(request.conversationId(), userId)
                        .orElseGet(() -> {
                            AssistantConversation c = new AssistantConversation();
                            c.setId(conversationId);
                            c.setUserId(userId);
                            c.setTitle(title);
                            return c;
                        });
                conversation.setPrompt(prompt);
                conversation.setResponse(safeResponse);
            } else {
                conversation = new AssistantConversation();
                conversation.setId(conversationId);
                conversation.setUserId(userId);
                conversation.setTitle(title);
                conversation.setPrompt(prompt);
                conversation.setResponse(safeResponse);
            }

            AssistantConversation saved = conversations.save(conversation);

            Instant now = Instant.now();
            // 8. Persist Individual Messages for Full Chat History
            AssistantMessage userMsg = new AssistantMessage();
            userMsg.setConversationId(conversationId);
            userMsg.setUserId(userId);
            userMsg.setRole("USER");
            userMsg.setContent(prompt);
            userMsg.setCreatedAt(now);
            messages.save(userMsg);

            AssistantMessage botMsg = new AssistantMessage();
            botMsg.setConversationId(conversationId);
            botMsg.setUserId(userId);
            botMsg.setRole("ASSISTANT");
            botMsg.setContent(safeResponse);
            botMsg.setCreatedAt(now.plusNanos(100_000));
            if (proposal != null) {
                try {
                    botMsg.setProposedActionJson(objectMapper.writeValueAsString(proposal));
                } catch (Exception ignored) {}
            }
            messages.save(botMsg);

            // 9. Return to Client
            return new AssistantChatResponse(
                    saved.getId(),
                    saved.getTitle(),
                    saved.getPrompt(),
                    saved.getResponse(),
                    proposal,
                    saved.getCreatedAt());

        } catch (Exception ex) {
            status = "ERROR";
            throw ex;
        } finally {
            // 10. Privacy-Safe Audit Logging
            int promptLen = request != null && request.prompt() != null ? request.prompt().length() : 0;
            auditLogger.logChatEvent(correlationId, userId, promptLen, status, System.currentTimeMillis() - startTime);
        }
    }

    @Transactional
    public AssistantActionExecutionResponse confirmAction(String userId, String actionId, AssistantActionExecutionRequest request) {
        return actionService.confirmAction(userId, actionId, request);
    }

    @Transactional
    public AssistantActionExecutionResponse cancelAction(String userId, String actionId) {
        return actionService.cancelAction(userId, actionId);
    }

    @Transactional
    public void deleteConversation(String userId, String conversationId) {
        AssistantConversation conversation = conversations.findByIdAndUserId(conversationId, userId)
                .orElseThrow(() -> new NotFoundException("Conversation not found"));
        messages.deleteByConversationIdAndUserId(conversationId, userId);
        conversations.delete(conversation);
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

    private AssistantMessageResponse toMessageResponse(AssistantMessage msg) {
        AssistantActionProposalResponse proposal = null;
        if (msg.getProposedActionJson() != null && !msg.getProposedActionJson().isBlank()) {
            try {
                proposal = objectMapper.readValue(msg.getProposedActionJson(), AssistantActionProposalResponse.class);
                if (proposal != null && proposal.actionId() != null) {
                    var actionOpt = actionService.getAction(msg.getUserId(), proposal.actionId());
                    if (actionOpt.isPresent()) {
                        var action = actionOpt.get();
                        proposal = new AssistantActionProposalResponse(
                                proposal.actionId(),
                                proposal.actionType(),
                                proposal.summary(),
                                proposal.parametersJson(),
                                proposal.requiresConfirmation(),
                                action.getStatus().name(),
                                proposal.expiresAt()
                        );
                    }
                }
            } catch (Exception ignored) {}
        }
        return new AssistantMessageResponse(
                msg.getId(),
                msg.getConversationId(),
                msg.getRole(),
                msg.getContent(),
                proposal,
                msg.getCreatedAt()
        );
    }
}

package com.dailymate.assistant.service;

import com.dailymate.assistant.dto.AssistantContext;
import com.dailymate.assistant.dto.request.AssistantActionExecutionRequest;
import com.dailymate.assistant.dto.request.AssistantChatRequest;
import com.dailymate.assistant.dto.response.AssistantActionExecutionResponse;
import com.dailymate.assistant.dto.response.AssistantActionProposalResponse;
import com.dailymate.assistant.dto.response.AssistantChatResponse;
import com.dailymate.assistant.dto.response.AssistantConversationResponse;
import com.dailymate.assistant.entity.AssistantConversation;
import com.dailymate.assistant.repository.AssistantConversationRepository;
import com.dailymate.assistant.security.AssistantAuditLogger;
import com.dailymate.assistant.security.AssistantPromptSanitizer;
import com.dailymate.assistant.security.AssistantRateLimiter;
import com.dailymate.assistant.security.AssistantResponseRedactor;
import com.dailymate.core.exception.NotFoundException;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AssistantService {

    private final AssistantConversationRepository conversations;
    private final AssistantContextService contextService;
    private final AssistantGroundingEngine groundingEngine;
    private final AssistantActionService actionService;
    private final AssistantRateLimiter rateLimiter;
    private final AssistantPromptSanitizer promptSanitizer;
    private final AssistantResponseRedactor responseRedactor;
    private final AssistantAuditLogger auditLogger;

    public AssistantService(
            AssistantConversationRepository conversations,
            AssistantContextService contextService,
            AssistantGroundingEngine groundingEngine,
            AssistantActionService actionService,
            AssistantRateLimiter rateLimiter,
            AssistantPromptSanitizer promptSanitizer,
            AssistantResponseRedactor responseRedactor,
            AssistantAuditLogger auditLogger) {
        this.conversations = conversations;
        this.contextService = contextService;
        this.groundingEngine = groundingEngine;
        this.actionService = actionService;
        this.rateLimiter = rateLimiter;
        this.promptSanitizer = promptSanitizer;
        this.responseRedactor = responseRedactor;
        this.auditLogger = auditLogger;
    }

    public List<AssistantConversationResponse> getConversations(String userId) {
        return conversations.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::toResponse)
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

            // 2. Prompt Security Check (Adversarial / Injection Rejection)
            if (promptSanitizer.isAdversarialOrRestricted(prompt)) {
                rawResponse = promptSanitizer.getSafeRejectionMessage();
            } else {
                // 3. Authorized Tenant Context Retrieval
                AssistantContext context = contextService.buildContext(userId);

                // 4. Grounding Engine with Tool Intent Parsing
                AssistantGroundingEngine.GroundingResult result = groundingEngine.process(prompt, userId, context);
                rawResponse = result.textResponse();

                if (result.proposal() != null) {
                    proposal = actionService.createProposal(
                            userId,
                            result.proposal().actionType(),
                            result.proposal().summary(),
                            result.proposal().parametersJson());
                }
            }

            // 5. Response Safety & Redaction Layer (Absolute Boundary)
            String safeResponse = responseRedactor.redact(rawResponse);

            // 6. Persistence of Redacted Safe Result (Same or New Conversation)
            String title = prompt.length() > 40 ? prompt.substring(0, 37) + "..." : prompt;

            AssistantConversation conversation;
            if (request.conversationId() != null && !request.conversationId().isBlank()) {
                conversation = conversations.findByIdAndUserId(request.conversationId(), userId)
                        .orElseGet(() -> {
                            AssistantConversation c = new AssistantConversation();
                            c.setUserId(userId);
                            c.setTitle(title);
                            return c;
                        });
                conversation.setPrompt(prompt);
                conversation.setResponse(safeResponse);
            } else {
                conversation = new AssistantConversation();
                conversation.setUserId(userId);
                conversation.setTitle(title);
                conversation.setPrompt(prompt);
                conversation.setResponse(safeResponse);
            }

            AssistantConversation saved = conversations.save(conversation);

            // 7. Return to Client
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
            // 8. Privacy-Safe Audit Logging
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
}

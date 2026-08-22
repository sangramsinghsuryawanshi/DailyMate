package com.dailymate.assistant.service;

import com.dailymate.assistant.dto.request.AssistantActionExecutionRequest;
import com.dailymate.assistant.dto.response.AssistantActionExecutionResponse;
import com.dailymate.assistant.dto.response.AssistantActionProposalResponse;
import com.dailymate.assistant.entity.AssistantAction;
import com.dailymate.assistant.entity.AssistantActionStatus;
import com.dailymate.assistant.repository.AssistantActionRepository;
import com.dailymate.assistant.security.AssistantAuditLogger;
import com.dailymate.assistant.tool.AssistantActionDispatcher;
import com.dailymate.core.exception.BadRequestException;
import com.dailymate.core.exception.ConflictException;
import com.dailymate.core.exception.NotFoundException;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AssistantActionService {

    private final AssistantActionRepository actions;
    private final AssistantActionDispatcher dispatcher;
    private final AssistantActionStateService stateService;
    private final AssistantAuditLogger auditLogger;

    public AssistantActionService(
            AssistantActionRepository actions,
            AssistantActionDispatcher dispatcher,
            AssistantActionStateService stateService,
            AssistantAuditLogger auditLogger) {
        this.actions = actions;
        this.dispatcher = dispatcher;
        this.stateService = stateService;
        this.auditLogger = auditLogger;
    }

    public AssistantActionProposalResponse createProposal(
            String userId,
            String actionType,
            String summary,
            String parametersJson) {
        String correlationId = UUID.randomUUID().toString();
        long startTime = System.currentTimeMillis();

        AssistantAction action = new AssistantAction();
        action.setUserId(userId);
        action.setActionType(actionType);
        action.setSummary(summary);
        action.setParametersJson(parametersJson);
        action.setStatus(AssistantActionStatus.PENDING);
        action.setExpiresAt(Instant.now().plus(Duration.ofMinutes(10)));

        AssistantAction saved = actions.save(action);

        auditLogger.logActionEvent(
                correlationId, saved.getId(), userId, actionType, "PENDING", "PROPOSED", System.currentTimeMillis() - startTime);

        return toProposalResponse(saved);
    }

    public AssistantActionExecutionResponse confirmAction(
            String userId,
            String actionId,
            AssistantActionExecutionRequest request) {
        String correlationId = UUID.randomUUID().toString();
        long startTime = System.currentTimeMillis();
        String idempotencyKey = request != null ? request.idempotencyKey() : null;

        // 1. Atomically Claim Action in separate transaction
        AssistantAction action;
        try {
            action = stateService.claimProcessing(actionId, userId, idempotencyKey);
        } catch (ConflictException ex) {
            // Handle concurrent PROCESSING with identical key outside transaction
            if (ex.getMessage() != null && ex.getMessage().contains("currently being processed") && idempotencyKey != null) {
                action = null;
                for (int i = 0; i < 30; i++) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                    Optional<AssistantAction> opt = actions.findByIdAndUserId(actionId, userId);
                    if (opt.isPresent() && opt.get().getStatus() == AssistantActionStatus.EXECUTED) {
                        action = opt.get();
                        break;
                    }
                }
            } else {
                throw ex;
            }
            if (action == null || action.getStatus() != AssistantActionStatus.EXECUTED) {
                throw ex;
            }
        }

        // 2. If already EXECUTED, replay stored result
        if (action.getStatus() == AssistantActionStatus.EXECUTED) {
            auditLogger.logActionEvent(
                    correlationId, actionId, userId, action.getActionType(), "EXECUTED", "REPLAYED", System.currentTimeMillis() - startTime);

            return new AssistantActionExecutionResponse(
                    action.getId(),
                    action.getActionType(),
                    "EXECUTED",
                    action.getResultMessage(),
                    action.getExecutedAt());
        }

        // 3. Dispatch to Domain Service & mark EXECUTED or FAILED
        try {
            String result = dispatcher.dispatch(userId, action.getActionType(), action.getParametersJson());
            AssistantAction executed = stateService.markExecuted(actionId, result);

            auditLogger.logActionEvent(
                    correlationId, actionId, userId, action.getActionType(), "EXECUTED", "EXECUTED", System.currentTimeMillis() - startTime);

            return new AssistantActionExecutionResponse(
                    executed.getId(),
                    executed.getActionType(),
                    "EXECUTED",
                    executed.getResultMessage(),
                    executed.getExecutedAt());

        } catch (Exception ex) {
            stateService.markFailed(actionId, ex.getMessage());

            auditLogger.logActionEvent(
                    correlationId, actionId, userId, action.getActionType(), "FAILED", "FAILED", System.currentTimeMillis() - startTime);

            throw ex;
        }
    }

    @Transactional
    public AssistantActionExecutionResponse cancelAction(String userId, String actionId) {
        String correlationId = UUID.randomUUID().toString();
        long startTime = System.currentTimeMillis();

        AssistantAction action = actions.findByIdAndUserId(actionId, userId)
                .orElseThrow(() -> new NotFoundException("Action proposal not found"));

        if (action.getStatus() == AssistantActionStatus.EXECUTED) {
            throw new ConflictException("Cannot cancel an action that has already been executed.");
        }

        action.setStatus(AssistantActionStatus.CANCELLED);
        actions.save(action);

        auditLogger.logActionEvent(
                correlationId, actionId, userId, action.getActionType(), "CANCELLED", "CANCELLED", System.currentTimeMillis() - startTime);

        return new AssistantActionExecutionResponse(
                action.getId(),
                action.getActionType(),
                "CANCELLED",
                "Action proposal cancelled.",
                Instant.now());
    }

    public AssistantActionProposalResponse toProposalResponse(AssistantAction action) {
        return new AssistantActionProposalResponse(
                action.getId(),
                action.getActionType(),
                action.getSummary(),
                action.getParametersJson(),
                true,
                action.getStatus().name(),
                action.getExpiresAt());
    }
}

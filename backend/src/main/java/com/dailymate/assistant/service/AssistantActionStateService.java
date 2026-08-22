package com.dailymate.assistant.service;

import com.dailymate.assistant.entity.AssistantAction;
import com.dailymate.assistant.entity.AssistantActionStatus;
import com.dailymate.assistant.repository.AssistantActionRepository;
import com.dailymate.assistant.security.AssistantResponseRedactor;
import com.dailymate.core.exception.BadRequestException;
import com.dailymate.core.exception.ConflictException;
import com.dailymate.core.exception.NotFoundException;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AssistantActionStateService {

    private final AssistantActionRepository actions;
    private final AssistantResponseRedactor redactor;

    public AssistantActionStateService(
            AssistantActionRepository actions,
            AssistantResponseRedactor redactor) {
        this.actions = actions;
        this.redactor = redactor;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AssistantAction claimProcessing(String actionId, String userId, String idempotencyKey) {
        AssistantAction action = actions.findByIdAndUserIdForUpdate(actionId, userId)
                .orElseThrow(() -> new NotFoundException("Action proposal not found"));

        // 1. Idempotency Check for already EXECUTED actions
        if (action.getStatus() == AssistantActionStatus.EXECUTED) {
            if (idempotencyKey != null && idempotencyKey.equals(action.getIdempotencyKey())) {
                return action; // Replay allowed
            }
            throw new ConflictException("Action has already been executed with a different or missing idempotency key.");
        }

        // 2. Concurrent PROCESSING handling
        if (action.getStatus() == AssistantActionStatus.PROCESSING) {
            throw new ConflictException("Action is currently being processed.");
        }

        // 3. Cancellation Check
        if (action.getStatus() == AssistantActionStatus.CANCELLED) {
            throw new BadRequestException("Action proposal has been cancelled and cannot be executed.");
        }

        // 4. Expiration Check
        if (action.getStatus() == AssistantActionStatus.EXPIRED || Instant.now().isAfter(action.getExpiresAt())) {
            action.setStatus(AssistantActionStatus.EXPIRED);
            actions.save(action);
            throw new BadRequestException("Action proposal has expired and cannot be executed.");
        }

        // 5. Invalid State Check
        if (action.getStatus() != AssistantActionStatus.PENDING) {
            throw new BadRequestException("Action proposal is in an invalid state: " + action.getStatus());
        }

        // 6. Atomic Claim
        action.setStatus(AssistantActionStatus.PROCESSING);
        action.setIdempotencyKey(idempotencyKey);
        return actions.saveAndFlush(action);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AssistantAction markExecuted(String actionId, String resultMessage) {
        AssistantAction action = actions.findById(actionId)
                .orElseThrow(() -> new NotFoundException("Action proposal not found"));
        action.setStatus(AssistantActionStatus.EXECUTED);
        action.setResultMessage(redactor.redact(resultMessage));
        action.setExecutedAt(Instant.now());
        return actions.saveAndFlush(action);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AssistantAction markFailed(String actionId, String rawErrorMessage) {
        AssistantAction action = actions.findById(actionId)
                .orElseThrow(() -> new NotFoundException("Action proposal not found"));
        action.setStatus(AssistantActionStatus.FAILED);
        String safeError = rawErrorMessage != null
                ? redactor.redact("Execution failed: " + rawErrorMessage)
                : "Execution failed due to an internal error.";
        action.setResultMessage(safeError);
        return actions.saveAndFlush(action);
    }
}

package com.dailymate.assistant.service;

import com.dailymate.assistant.entity.AssistantActionStatus;
import com.dailymate.assistant.repository.AssistantActionRepository;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Scheduled and callable cleanup service for stale assistant action records.
 * Invariant: Never deletes PENDING proposals, active PROCESSING actions, or recent EXECUTED records within the replay window.
 */
@Service
public class AssistantActionCleanupService {

    private static final Logger log = LoggerFactory.getLogger(AssistantActionCleanupService.class);

    private final AssistantActionRepository actions;

    public AssistantActionCleanupService(AssistantActionRepository actions) {
        this.actions = actions;
    }

    @Transactional
    public int purgeTerminalActions(Instant cutoff) {
        int purged = actions.deleteByExpiresAtBeforeAndStatusIn(
                cutoff,
                List.of(AssistantActionStatus.EXPIRED, AssistantActionStatus.CANCELLED, AssistantActionStatus.FAILED)
        );
        log.info("PURGE_TERMINAL_ACTIONS count={} cutoff={}", purged, cutoff);
        return purged;
    }

    @Transactional
    public int purgeStaleExecutedActions(Instant cutoff) {
        int purged = actions.deleteByExecutedAtBeforeAndStatus(cutoff, AssistantActionStatus.EXECUTED);
        log.info("PURGE_STALE_EXECUTED_ACTIONS count={} cutoff={}", purged, cutoff);
        return purged;
    }

    @Transactional
    public int purgeAllStaleActions(Instant terminalCutoff, Instant executedCutoff) {
        return purgeTerminalActions(terminalCutoff) + purgeStaleExecutedActions(executedCutoff);
    }
}

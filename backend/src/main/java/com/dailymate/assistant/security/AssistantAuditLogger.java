package com.dailymate.assistant.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Privacy-safe structured audit logger for AI assistant interactions and tool executions.
 * Invariant: Never logs raw prompt content, PII, medical records, financial amounts, or secrets.
 */
@Component
public class AssistantAuditLogger {

    private static final Logger log = LoggerFactory.getLogger("ASSISTANT_AUDIT");

    public void logChatEvent(String correlationId, String userId, int promptLength, String status, long durationMs) {
        log.info("ASSISTANT_INTERACTION correlationId={} userId={} promptLength={} status={} durationMs={}",
                correlationId, userId, promptLength, status, durationMs);
    }

    public void logActionEvent(
            String correlationId,
            String actionId,
            String userId,
            String actionType,
            String status,
            String outcome,
            long durationMs) {
        log.info("ASSISTANT_ACTION correlationId={} actionId={} userId={} actionType={} status={} outcome={} durationMs={}",
                correlationId, actionId, userId, actionType, status, outcome, durationMs);
    }
}

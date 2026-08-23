package com.dailymate.assistant.domain;

/**
 * Encapsulates an isolated segment of a user prompt belonging to a specific domain.
 */
public record DomainSegment(
        AssistantDomain domain,
        String sectionLabel,
        String payload) {
}

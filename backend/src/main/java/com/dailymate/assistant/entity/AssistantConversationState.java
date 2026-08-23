package com.dailymate.assistant.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "assistant_conversation_state")
public class AssistantConversationState {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "conversation_id", nullable = false, unique = true, length = 36)
    private String conversationId;

    @Column(name = "user_id", nullable = false, length = 36)
    private String userId;

    @Column(name = "active_proposal_id", length = 36)
    private String activeProposalId;

    @Column(name = "last_action_id", length = 36)
    private String lastActionId;

    @Column(name = "last_tool_name", length = 120)
    private String lastToolName;

    @Column(name = "last_entity_type", length = 64)
    private String lastEntityType;

    @Column(name = "last_entity_id", length = 64)
    private String lastEntityId;

    @Column(name = "last_entity_summary", length = 255)
    private String lastEntitySummary;

    @Column(name = "unresolved_params_json", columnDefinition = "LONGTEXT")
    private String unresolvedParamsJson;

    @Column(name = "turn_count", nullable = false)
    private int turnCount = 1;

    @Version
    @Column(name = "version", nullable = false)
    private Long version = 0L;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    public AssistantConversationState() {
        this.id = UUID.randomUUID().toString();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getConversationId() { return conversationId; }
    public void setConversationId(String conversationId) { this.conversationId = conversationId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getActiveProposalId() { return activeProposalId; }
    public void setActiveProposalId(String activeProposalId) { this.activeProposalId = activeProposalId; }

    public String getLastActionId() { return lastActionId; }
    public void setLastActionId(String lastActionId) { this.lastActionId = lastActionId; }

    public String getLastToolName() { return lastToolName; }
    public void setToolName(String lastToolName) { this.lastToolName = lastToolName; }

    public String getLastEntityType() { return lastEntityType; }
    public void setLastEntityType(String lastEntityType) { this.lastEntityType = lastEntityType; }

    public String getLastEntityId() { return lastEntityId; }
    public void setLastEntityId(String lastEntityId) { this.lastEntityId = lastEntityId; }

    public String getLastEntitySummary() { return lastEntitySummary; }
    public void setLastEntitySummary(String lastEntitySummary) { this.lastEntitySummary = lastEntitySummary; }

    public String getUnresolvedParamsJson() { return unresolvedParamsJson; }
    public void setUnresolvedParamsJson(String unresolvedParamsJson) { this.unresolvedParamsJson = unresolvedParamsJson; }

    public int getTurnCount() { return turnCount; }
    public void setTurnCount(int turnCount) { this.turnCount = turnCount; }

    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
}

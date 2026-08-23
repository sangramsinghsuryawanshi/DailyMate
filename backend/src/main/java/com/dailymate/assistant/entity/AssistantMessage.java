package com.dailymate.assistant.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "assistant_messages")
public class AssistantMessage {

    @Id
    @Column(length = 64)
    private String id;

    @Column(name = "conversation_id", nullable = false, length = 36)
    private String conversationId;

    @Column(name = "user_id", nullable = false, length = 36)
    private String userId;

    @Column(name = "role", nullable = false, length = 20)
    private String role; // "USER" or "ASSISTANT"

    @Column(name = "content", nullable = false, columnDefinition = "LONGTEXT")
    private String content;

    @Column(name = "proposed_action_json", columnDefinition = "LONGTEXT")
    private String proposedActionJson;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public AssistantMessage() {
        this.id = UUID.randomUUID().toString();
    }

    @PrePersist
    void onCreate() {
        if (id == null) id = UUID.randomUUID().toString();
        if (createdAt == null) createdAt = Instant.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getConversationId() { return conversationId; }
    public void setConversationId(String conversationId) { this.conversationId = conversationId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getProposedActionJson() { return proposedActionJson; }
    public void setProposedActionJson(String proposedActionJson) { this.proposedActionJson = proposedActionJson; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}

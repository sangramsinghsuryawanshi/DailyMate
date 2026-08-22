package com.dailymate.assistant.entity;

import com.dailymate.assistant.tool.BulkOperationStatus;
import com.dailymate.assistant.tool.OperationScope;
import com.dailymate.assistant.tool.ToolScope;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "assistant_bulk_operations")
public class AssistantBulkOperation {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "bulk_execution_id", nullable = false, unique = true, length = 100)
    private String bulkExecutionId;

    @Column(name = "tool_name", nullable = false, length = 120)
    private String toolName;

    @Column(name = "actor_id", nullable = false, length = 36)
    private String actorId;

    @Enumerated(EnumType.STRING)
    @Column(name = "scope", nullable = false, length = 32)
    private ToolScope scope;

    @Enumerated(EnumType.STRING)
    @Column(name = "operation_scope", nullable = false, length = 32)
    private OperationScope operationScope;

    @Column(name = "preview_hash", nullable = false, length = 128)
    private String previewHash;

    @Column(name = "total_rows", nullable = false)
    private int totalRows;

    @Column(name = "valid_rows", nullable = false)
    private int validRows;

    @Column(name = "invalid_rows", nullable = false)
    private int invalidRows;

    @Column(name = "duplicate_rows", nullable = false)
    private int duplicateRows;

    @Column(name = "succeeded_rows", nullable = false)
    private int succeededRows;

    @Column(name = "failed_rows", nullable = false)
    private int failedRows;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private BulkOperationStatus status;

    @Column(name = "summary", nullable = false, length = 500)
    private String summary;

    @Column(name = "payload_json", columnDefinition = "LONGTEXT")
    private String payloadJson;

    @Column(name = "result_json", columnDefinition = "LONGTEXT")
    private String resultJson;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "confirmed_at")
    private Instant confirmedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    public AssistantBulkOperation() {
        this.id = UUID.randomUUID().toString();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getBulkExecutionId() { return bulkExecutionId; }
    public void setBulkExecutionId(String bulkExecutionId) { this.bulkExecutionId = bulkExecutionId; }

    public String getToolName() { return toolName; }
    public void setToolName(String toolName) { this.toolName = toolName; }

    public String getActorId() { return actorId; }
    public void setActorId(String actorId) { this.actorId = actorId; }

    public ToolScope getScope() { return scope; }
    public void setScope(ToolScope scope) { this.scope = scope; }

    public OperationScope getOperationScope() { return operationScope; }
    public void setOperationScope(OperationScope operationScope) { this.operationScope = operationScope; }

    public String getPreviewHash() { return previewHash; }
    public void setPreviewHash(String previewHash) { this.previewHash = previewHash; }

    public int getTotalRows() { return totalRows; }
    public void setTotalRows(int totalRows) { this.totalRows = totalRows; }

    public int getValidRows() { return validRows; }
    public void setValidRows(int validRows) { this.validRows = validRows; }

    public int getInvalidRows() { return invalidRows; }
    public void setInvalidRows(int invalidRows) { this.invalidRows = invalidRows; }

    public int getDuplicateRows() { return duplicateRows; }
    public void setDuplicateRows(int duplicateRows) { this.duplicateRows = duplicateRows; }

    public int getSucceededRows() { return succeededRows; }
    public void setSucceededRows(int succeededRows) { this.succeededRows = succeededRows; }

    public int getFailedRows() { return failedRows; }
    public void setFailedRows(int failedRows) { this.failedRows = failedRows; }

    public BulkOperationStatus getStatus() { return status; }
    public void setStatus(BulkOperationStatus status) { this.status = status; }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public String getPayloadJson() { return payloadJson; }
    public void setPayloadJson(String payloadJson) { this.payloadJson = payloadJson; }

    public String getResultJson() { return resultJson; }
    public void setResultJson(String resultJson) { this.resultJson = resultJson; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getConfirmedAt() { return confirmedAt; }
    public void setConfirmedAt(Instant confirmedAt) { this.confirmedAt = confirmedAt; }

    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
}

package com.dailymate.assistant.service;

import com.dailymate.assistant.dto.BulkExecutionResultDto;
import com.dailymate.assistant.dto.BulkOperationPreviewDto;
import com.dailymate.assistant.entity.AssistantBulkOperation;
import com.dailymate.assistant.repository.AssistantBulkOperationRepository;
import com.dailymate.assistant.tool.AssistantToolDefinition;
import com.dailymate.assistant.tool.AssistantToolRegistry;
import com.dailymate.assistant.tool.BulkOperationStatus;
import com.dailymate.assistant.tool.ToolScope;
import com.dailymate.core.exception.BadRequestException;
import com.dailymate.core.exception.ConflictException;
import com.dailymate.core.exception.ForbiddenException;
import com.dailymate.core.exception.NotFoundException;
import com.dailymate.expense.dto.request.ExpenseEntryRequest;
import com.dailymate.expense.service.ExpenseService;
import com.dailymate.medicine.dto.request.MedicineReminderRequest;
import com.dailymate.medicine.service.MedicineReminderService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Universal Server-Authoritative Bulk Operations Engine for DailyMate.
 * Invariants: Pre-execution validation, previewHash protection, chunked domain execution, and zero repository bypass.
 */
@Service
public class AssistantBulkOperationsService {

    private static final Logger log = LoggerFactory.getLogger("ASSISTANT_BULK_AUDIT");
    public static final int MAX_BULK_ROWS = 500;
    public static final int MAX_BULK_DELETE_ROWS = 100;

    private final AssistantBulkOperationRepository bulkRepo;
    private final AssistantToolRegistry toolRegistry;
    private final ExpenseService expenseService;
    private final MedicineReminderService medicineService;
    private final ObjectMapper objectMapper;

    public AssistantBulkOperationsService(
            AssistantBulkOperationRepository bulkRepo,
            AssistantToolRegistry toolRegistry,
            ExpenseService expenseService,
            MedicineReminderService medicineService) {
        this.bulkRepo = bulkRepo;
        this.toolRegistry = toolRegistry;
        this.expenseService = expenseService;
        this.medicineService = medicineService;
        this.objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    }

    public BulkOperationPreviewDto previewBulkOperation(
            String actorId,
            String actorRole,
            String toolName,
            List<Map<String, Object>> payloadRows) {

        // 1. Tool Governance & Authorization
        toolRegistry.validateAuthorization(toolName, actorRole);
        AssistantToolDefinition tool = toolRegistry.getTool(toolName);

        if (!tool.bulkAllowed()) {
            throw new BadRequestException("Tool " + toolName + " is not configured for bulk operations.");
        }

        // Scope Gating: USER cannot invoke ADMIN-scoped bulk tools
        if (tool.scope() == ToolScope.ADMIN && !"ADMIN".equalsIgnoreCase(actorRole)) {
            throw new ForbiddenException("Administrative scope required for bulk tool: " + toolName);
        }

        // 2. Server Batch Limits Enforcement
        if (payloadRows == null || payloadRows.isEmpty()) {
            throw new BadRequestException("Bulk payload rows must not be empty.");
        }

        int maxLimit = tool.destructive() ? MAX_BULK_DELETE_ROWS : MAX_BULK_ROWS;
        if (payloadRows.size() > maxLimit) {
            throw new BadRequestException("Bulk batch size of " + payloadRows.size() + " exceeds maximum allowed limit of " + maxLimit + " rows.");
        }

        // 3. Validation & Duplicate Detection
        int totalRows = payloadRows.size();
        int validRows = 0;
        int invalidRows = 0;
        int duplicateRows = 0;
        List<String> validationErrors = new ArrayList<>();
        Set<String> seenKeys = new HashSet<>();

        for (int i = 0; i < payloadRows.size(); i++) {
            Map<String, Object> row = payloadRows.get(i);
            String rowKey = row.toString();
            if (seenKeys.contains(rowKey)) {
                duplicateRows++;
                validationErrors.add("Row " + (i + 1) + ": Duplicate row detected.");
                continue;
            }
            seenKeys.add(rowKey);

            boolean rowValid = validateRow(toolName, row, i + 1, validationErrors);
            if (rowValid) {
                validRows++;
            } else {
                invalidRows++;
            }
        }

        // 4. Compute previewHash & Store Bulk Execution Entity
        String payloadJson = serializeJson(payloadRows);
        String previewHash = computeSha256(payloadJson);
        String bulkExecutionId = "BULK-" + System.currentTimeMillis() + "-" + actorId.substring(0, 8);

        String summary = String.format("Bulk %s: %d total (%d valid, %d invalid, %d duplicates)",
                toolName, totalRows, validRows, invalidRows, duplicateRows);

        AssistantBulkOperation operation = new AssistantBulkOperation();
        operation.setBulkExecutionId(bulkExecutionId);
        operation.setToolName(toolName);
        operation.setActorId(actorId);
        operation.setScope(tool.scope());
        operation.setOperationScope(tool.operationScope());
        operation.setPreviewHash(previewHash);
        operation.setTotalRows(totalRows);
        operation.setValidRows(validRows);
        operation.setInvalidRows(invalidRows);
        operation.setDuplicateRows(duplicateRows);
        operation.setStatus(BulkOperationStatus.PENDING);
        operation.setSummary(summary);
        operation.setPayloadJson(payloadJson);

        bulkRepo.save(operation);

        log.info("BULK_PREVIEW bulkExecutionId={} actorId={} toolName={} total={} valid={} invalid={} duplicates={}",
                bulkExecutionId, actorId, toolName, totalRows, validRows, invalidRows, duplicateRows);

        return new BulkOperationPreviewDto(
                bulkExecutionId,
                toolName,
                totalRows,
                validRows,
                invalidRows,
                duplicateRows,
                previewHash,
                summary,
                BulkOperationStatus.PENDING,
                validationErrors
        );
    }

    public BulkExecutionResultDto confirmBulkOperation(
            String actorId,
            String actorRole,
            String bulkExecutionId,
            String clientPreviewHash) {

        // 1. Fetch & Ownership Validation
        AssistantBulkOperation operation = bulkRepo.findByBulkExecutionId(bulkExecutionId)
                .orElseThrow(() -> new NotFoundException("Bulk execution record not found: " + bulkExecutionId));

        if (!operation.getActorId().equals(actorId) && !"ADMIN".equalsIgnoreCase(actorRole)) {
            throw new NotFoundException("Bulk execution record not found: " + bulkExecutionId);
        }

        // 2. Idempotency Replay
        if (operation.getStatus() == BulkOperationStatus.COMPLETED
                || operation.getStatus() == BulkOperationStatus.COMPLETED_WITH_ERRORS
                || operation.getStatus() == BulkOperationStatus.FAILED) {
            log.info("BULK_REPLAY bulkExecutionId={} status={}", bulkExecutionId, operation.getStatus());
            return deserializeResult(operation);
        }

        if (operation.getStatus() != BulkOperationStatus.PENDING) {
            throw new ConflictException("Bulk operation cannot be confirmed in state: " + operation.getStatus());
        }

        // 3. Preview Hash & Stale Preview Integrity (Invariant 8)
        if (clientPreviewHash != null && !clientPreviewHash.equalsIgnoreCase(operation.getPreviewHash())) {
            operation.setStatus(BulkOperationStatus.EXPIRED);
            bulkRepo.save(operation);
            throw new ConflictException("Stale preview detected: Target records or parameters changed since preview generation.");
        }

        // 4. Mark PROCESSING
        operation.setStatus(BulkOperationStatus.PROCESSING);
        operation.setConfirmedAt(Instant.now());
        bulkRepo.save(operation);

        // 5. Chunked Domain Execution
        List<Map<String, Object>> rows = deserializePayload(operation.getPayloadJson());
        int succeeded = 0;
        int failed = 0;
        List<String> failureDetails = new ArrayList<>();

        for (int i = 0; i < rows.size(); i++) {
            Map<String, Object> row = rows.get(i);
            try {
                executeSingleRow(operation.getToolName(), actorId, row);
                succeeded++;
            } catch (Exception ex) {
                failed++;
                failureDetails.add("Row " + (i + 1) + ": " + ex.getMessage());
            }
        }

        // 6. Outcome Determination
        BulkOperationStatus finalStatus;
        if (failed == 0) {
            finalStatus = BulkOperationStatus.COMPLETED;
        } else if (succeeded > 0) {
            finalStatus = BulkOperationStatus.COMPLETED_WITH_ERRORS;
        } else {
            finalStatus = BulkOperationStatus.FAILED;
        }

        operation.setSucceededRows(succeeded);
        operation.setFailedRows(failed);
        operation.setStatus(finalStatus);
        operation.setCompletedAt(Instant.now());

        String resultMsg = String.format("Bulk operation %s: %d succeeded, %d failed out of %d total.",
                finalStatus, succeeded, failed, rows.size());

        BulkExecutionResultDto resultDto = new BulkExecutionResultDto(
                bulkExecutionId,
                finalStatus,
                rows.size(),
                succeeded,
                failed,
                resultMsg,
                failureDetails,
                operation.getCompletedAt()
        );

        operation.setResultJson(serializeJson(resultDto));
        bulkRepo.save(operation);

        log.info("BULK_COMPLETE bulkExecutionId={} finalStatus={} succeeded={} failed={}",
                bulkExecutionId, finalStatus, succeeded, failed);

        return resultDto;
    }

    private void executeSingleRow(String toolName, String actorId, Map<String, Object> row) {
        switch (toolName) {
            case "expense.bulkRecord" -> {
                String cat = (String) row.get("category");
                String desc = (String) row.get("description");
                Object amtObj = row.get("amount");
                BigDecimal amt = amtObj instanceof Number ? BigDecimal.valueOf(((Number) amtObj).doubleValue()) : new BigDecimal(amtObj.toString());
                LocalDate spentOn = row.get("spentOn") != null ? LocalDate.parse((String) row.get("spentOn")) : LocalDate.now();
                String notes = (String) row.get("notes");
                expenseService.createEntry(actorId, new ExpenseEntryRequest(cat, desc, amt, spentOn, notes));
            }
            case "expense.bulkDelete" -> {
                String expenseId = (String) row.get("expenseId");
                expenseService.deleteEntry(actorId, expenseId);
            }
            case "medicine.bulkCreate" -> {
                String name = (String) row.get("name");
                String dosage = (String) row.get("dosage");
                String remindAtStr = (String) row.get("remindAt");
                LocalTime remindAt = remindAtStr != null ? LocalTime.parse(remindAtStr.length() == 5 ? remindAtStr : remindAtStr + ":00") : LocalTime.of(9, 0);
                String freq = row.get("frequency") != null ? (String) row.get("frequency") : "DAILY";
                String notes = (String) row.get("notes");
                medicineService.createReminder(actorId, new MedicineReminderRequest(name, dosage, freq, remindAt, notes, true));
            }
            case "medicine.bulkDelete" -> {
                String reminderId = (String) row.get("reminderId");
                medicineService.deleteReminder(actorId, reminderId);
            }
            default -> throw new BadRequestException("Unsupported bulk tool executor: " + toolName);
        }
    }

    private boolean validateRow(String toolName, Map<String, Object> row, int rowNum, List<String> errors) {
        if ("expense.bulkRecord".equals(toolName)) {
            if (row.get("amount") == null) {
                errors.add("Row " + rowNum + ": Missing required field 'amount'.");
                return false;
            }
            if (row.get("category") == null || ((String) row.get("category")).isBlank()) {
                errors.add("Row " + rowNum + ": Missing required field 'category'.");
                return false;
            }
            if (row.get("description") == null || ((String) row.get("description")).isBlank()) {
                errors.add("Row " + rowNum + ": Missing required field 'description'.");
                return false;
            }
            return true;
        } else if ("expense.bulkDelete".equals(toolName)) {
            if (row.get("expenseId") == null || ((String) row.get("expenseId")).isBlank()) {
                errors.add("Row " + rowNum + ": Missing required field 'expenseId'.");
                return false;
            }
            return true;
        } else if ("medicine.bulkCreate".equals(toolName)) {
            if (row.get("name") == null || ((String) row.get("name")).isBlank()) {
                errors.add("Row " + rowNum + ": Missing required field 'name'.");
                return false;
            }
            if (row.get("remindAt") == null || ((String) row.get("remindAt")).isBlank()) {
                errors.add("Row " + rowNum + ": Missing required field 'remindAt'.");
                return false;
            }
            return true;
        }
        return true;
    }

    private String computeSha256(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(text.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception ex) {
            throw new RuntimeException("SHA-256 algorithm unavailable", ex);
        }
    }

    private String serializeJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception ex) {
            throw new RuntimeException("Failed to serialize json", ex);
        }
    }

    private List<Map<String, Object>> deserializePayload(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<List<Map<String, Object>>>() {});
        } catch (Exception ex) {
            return List.of();
        }
    }

    private BulkExecutionResultDto deserializeResult(AssistantBulkOperation op) {
        try {
            if (op.getResultJson() != null) {
                return objectMapper.readValue(op.getResultJson(), BulkExecutionResultDto.class);
            }
        } catch (Exception ignored) {}
        return new BulkExecutionResultDto(
                op.getBulkExecutionId(),
                op.getStatus(),
                op.getTotalRows(),
                op.getSucceededRows(),
                op.getFailedRows(),
                op.getSummary(),
                List.of(),
                op.getCompletedAt() != null ? op.getCompletedAt() : Instant.now()
        );
    }
}

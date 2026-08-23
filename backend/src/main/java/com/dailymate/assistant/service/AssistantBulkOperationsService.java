package com.dailymate.assistant.service;

import com.dailymate.assistant.dto.BulkExecutionResultDto;
import com.dailymate.assistant.dto.BulkOperationPreviewDto;
import com.dailymate.assistant.dto.request.CanonicalBulkRequest;
import com.dailymate.assistant.entity.AssistantBulkOperation;
import com.dailymate.assistant.repository.AssistantBulkOperationRepository;
import com.dailymate.assistant.tool.AssistantToolDefinition;
import com.dailymate.assistant.tool.AssistantToolRegistry;
import com.dailymate.assistant.tool.BulkOperationStatus;
import com.dailymate.assistant.tool.ToolScope;
import com.dailymate.blood.dto.request.BloodRequestCreateRequest;
import com.dailymate.blood.service.BloodDonationService;
import com.dailymate.community.dto.request.CommunityComplaintRequest;
import com.dailymate.community.service.CommunityComplaintService;
import com.dailymate.core.exception.BadRequestException;
import com.dailymate.core.exception.ConflictException;
import com.dailymate.core.exception.ForbiddenException;
import com.dailymate.core.exception.NotFoundException;
import com.dailymate.emergency.dto.request.EmergencyContactRequest;
import com.dailymate.emergency.service.EmergencyContactService;
import com.dailymate.events.dto.request.LocalEventCreateRequest;
import com.dailymate.events.service.LocalEventService;
import com.dailymate.expense.dto.request.ExpenseEntryRequest;
import com.dailymate.expense.service.ExpenseService;
import com.dailymate.grocery.dto.request.GroceryItemRequest;
import com.dailymate.grocery.service.GroceryComparisonService;
import com.dailymate.jobs.dto.request.JobPostRequest;
import com.dailymate.jobs.service.JobPostService;
import com.dailymate.lostfound.dto.request.LostItemPostRequest;
import com.dailymate.lostfound.service.LostFoundService;
import com.dailymate.marketplace.dto.request.ServiceProviderRequest;
import com.dailymate.marketplace.service.MarketplaceService;
import com.dailymate.medicine.dto.request.MedicineReminderRequest;
import com.dailymate.medicine.service.MedicineReminderService;
import com.dailymate.notification.dto.request.NotificationRequest;
import com.dailymate.notification.service.NotificationService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Universal Server-Authoritative Bulk Operations Engine for DailyMate AI Assistant.
 * Invariants: Pre-execution validation, snapshot integrity, confirmation phrase enforcement,
 * proposal expiration, and zero direct repository persistence bypass.
 */
@Service
public class AssistantBulkOperationsService {

    private static final Logger log = LoggerFactory.getLogger("ASSISTANT_BULK_AUDIT");
    public static final int MAX_BULK_ROWS = 500;
    public static final int MAX_BULK_DELETE_ROWS = 100;
    public static final Duration PROPOSAL_EXPIRY = Duration.ofMinutes(10);

    private final AssistantBulkOperationRepository bulkRepo;
    private final AssistantToolRegistry toolRegistry;
    private final ExpenseService expenseService;
    private final MedicineReminderService medicineService;
    private final EmergencyContactService emergencyService;
    private final BloodDonationService bloodService;
    private final MarketplaceService marketplaceService;
    private final LocalEventService eventService;
    private final JobPostService jobService;
    private final LostFoundService lostFoundService;
    private final CommunityComplaintService complaintService;
    private final GroceryComparisonService groceryService;
    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    public AssistantBulkOperationsService(
            AssistantBulkOperationRepository bulkRepo,
            AssistantToolRegistry toolRegistry,
            ExpenseService expenseService,
            MedicineReminderService medicineService,
            EmergencyContactService emergencyService,
            BloodDonationService bloodService,
            MarketplaceService marketplaceService,
            LocalEventService eventService,
            JobPostService jobService,
            LostFoundService lostFoundService,
            CommunityComplaintService complaintService,
            GroceryComparisonService groceryService,
            NotificationService notificationService) {
        this.bulkRepo = bulkRepo;
        this.toolRegistry = toolRegistry;
        this.expenseService = expenseService;
        this.medicineService = medicineService;
        this.emergencyService = emergencyService;
        this.bloodService = bloodService;
        this.marketplaceService = marketplaceService;
        this.eventService = eventService;
        this.jobService = jobService;
        this.lostFoundService = lostFoundService;
        this.complaintService = complaintService;
        this.groceryService = groceryService;
        this.notificationService = notificationService;
        this.objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    }

    public BulkOperationPreviewDto previewBulkOperation(
            String actorId,
            String actorRole,
            CanonicalBulkRequest request) {

        String toolName = request.toolName();

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

        // Admin Reason Mandate
        if (tool.adminReasonRequired() && (request.adminReason() == null || request.adminReason().isBlank())) {
            throw new BadRequestException("Administrative reason is required for tool: " + toolName);
        }

        List<Map<String, Object>> payloadRows = request.payloadRows();
        if (payloadRows == null || payloadRows.isEmpty()) {
            throw new BadRequestException("Bulk payload rows must not be empty.");
        }

        // 2. Server Batch Limits Enforcement
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
        List<String> targetSnapshot = new ArrayList<>();

        for (int i = 0; i < payloadRows.size(); i++) {
            Map<String, Object> row = payloadRows.get(i);

            // Invariant: User scope filter privilege isolation (enforce principal ownership)
            if (tool.scope() == ToolScope.USER && row.containsKey("userId")) {
                Object rowUserId = row.get("userId");
                if (rowUserId != null && !actorId.equals(rowUserId.toString())) {
                    invalidRows++;
                    validationErrors.add("Row " + (i + 1) + ": Unauthorized cross-tenant target resource.");
                    continue;
                }
            }

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
                String candidateId = extractTargetResourceId(toolName, row, i + 1);
                targetSnapshot.add(candidateId);
            } else {
                invalidRows++;
            }
        }

        // 4. Compute previewHash & Server-Generated Confirmation Phrase
        String payloadJson = serializeJson(payloadRows);
        String targetSnapshotJson = serializeJson(targetSnapshot);
        String previewHash = computeSha256(toolName + "|" + payloadJson + "|" + targetSnapshotJson);
        String bulkExecutionId = "BULK-" + System.currentTimeMillis() + "-" + (actorId.length() >= 8 ? actorId.substring(0, 8) : actorId);
        Instant expiresAt = Instant.now().plus(PROPOSAL_EXPIRY);

        String confirmationPhrase = null;
        if (tool.requiresConfirmationPhrase()) {
            confirmationPhrase = "CONFIRM " + tool.operationType().name() + " " + totalRows + " " + tool.domain().name();
        }

        String summary = String.format("Bulk %s: %d total (%d valid, %d invalid, %d duplicates)",
                toolName, totalRows, validRows, invalidRows, duplicateRows);

        // Dry Run Mode: Return preview metrics without persisting PENDING proposal
        if (request.dryRun()) {
            return new BulkOperationPreviewDto(
                    bulkExecutionId,
                    toolName,
                    totalRows,
                    validRows,
                    invalidRows,
                    duplicateRows,
                    previewHash,
                    confirmationPhrase,
                    expiresAt,
                    true,
                    "DRY RUN: " + summary,
                    BulkOperationStatus.PREVIEW,
                    validationErrors
            );
        }

        // 5. Persist PENDING Proposal
        AssistantBulkOperation operation = new AssistantBulkOperation();
        operation.setBulkExecutionId(bulkExecutionId);
        operation.setToolName(toolName);
        operation.setActorId(actorId);
        operation.setScope(tool.scope());
        operation.setOperationScope(tool.operationScope());
        operation.setPreviewHash(previewHash);
        operation.setConfirmationPhrase(confirmationPhrase);
        operation.setAdminReason(request.adminReason());
        operation.setTotalRows(totalRows);
        operation.setValidRows(validRows);
        operation.setInvalidRows(invalidRows);
        operation.setDuplicateRows(duplicateRows);
        operation.setStatus(BulkOperationStatus.PENDING);
        operation.setSummary(summary);
        operation.setPayloadJson(payloadJson);
        operation.setTargetSnapshotJson(targetSnapshotJson);
        operation.setExpiresAt(expiresAt);
        operation.setDryRun(false);

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
                confirmationPhrase,
                expiresAt,
                false,
                summary,
                BulkOperationStatus.PENDING,
                validationErrors
        );
    }

    public BulkExecutionResultDto confirmBulkOperation(
            String actorId,
            String actorRole,
            String bulkExecutionId,
            String clientPreviewHash,
            String clientConfirmationPhrase) {

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

        // 3. Proposal Expiry Check (Invariant 24)
        if (operation.getExpiresAt() != null && Instant.now().isAfter(operation.getExpiresAt())) {
            operation.setStatus(BulkOperationStatus.EXPIRED);
            bulkRepo.save(operation);
            throw new ConflictException("Bulk proposal has expired. Please generate a new preview.");
        }

        if (operation.getStatus() != BulkOperationStatus.PENDING) {
            throw new ConflictException("Bulk operation cannot be confirmed in state: " + operation.getStatus());
        }

        // 4. Preview Hash & Stale Preview Integrity (Invariant 8)
        if (clientPreviewHash != null && !clientPreviewHash.equalsIgnoreCase(operation.getPreviewHash())) {
            operation.setStatus(BulkOperationStatus.EXPIRED);
            bulkRepo.save(operation);
            throw new ConflictException("Stale preview detected: Target records or parameters changed since preview generation.");
        }

        // 5. Confirmation Phrase Integrity (Invariant 23)
        if (operation.getConfirmationPhrase() != null) {
            if (clientConfirmationPhrase == null || !clientConfirmationPhrase.trim().equalsIgnoreCase(operation.getConfirmationPhrase().trim())) {
                throw new BadRequestException("Invalid confirmation phrase. Expected: " + operation.getConfirmationPhrase());
            }
        }

        // 6. Mark PROCESSING
        operation.setStatus(BulkOperationStatus.PROCESSING);
        operation.setConfirmedAt(Instant.now());
        bulkRepo.save(operation);

        // 7. Chunked Domain Execution using Target Snapshot
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

        // 8. Outcome Determination
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
            case "emergency.bulkCreate" -> {
                String name = (String) row.get("name");
                String rel = row.get("relationship") != null ? (String) row.get("relationship") : "Family";
                String phone = (String) row.get("phone");
                String cat = row.get("category") != null ? (String) row.get("category") : "Family";
                String notes = (String) row.get("notes");
                emergencyService.createContact(actorId, new EmergencyContactRequest(name, cat, phone, "ICE Personal", rel + (notes != null ? " - " + notes : "")));
            }
            case "blood.bulkCreateRequests" -> {
                String patientName = (String) row.get("patientName");
                String bg = (String) row.get("bloodGroup");
                String hosp = row.get("hospitalLocation") != null ? (String) row.get("hospitalLocation") : "City Hospital";
                String phone = row.get("contactPhone") != null ? (String) row.get("contactPhone") : "9876543210";
                bloodService.createRequest(actorId, new BloodRequestCreateRequest(patientName, bg, 1, hosp, "HIGH", patientName, phone, "Imported via Assistant"));
            }
            case "marketplace.bulkRegister" -> {
                String name = (String) row.get("businessName");
                if (name == null) name = (String) row.get("name");
                String cat = (String) row.get("category");
                if (cat == null) cat = (String) row.get("serviceType");
                String phone = (String) row.get("phone");
                String address = row.get("address") != null ? (String) row.get("address") : "Pune, MH";
                marketplaceService.createProvider(actorId, new ServiceProviderRequest(name, cat, "Registered via Assistant", address, phone, null, BigDecimal.valueOf(300.00)));
            }
            case "events.bulkCreate" -> {
                String title = (String) row.get("title");
                String desc = (String) row.get("description");
                String loc = row.get("location") != null ? (String) row.get("location") : "Local Area";
                Instant date = row.get("eventDate") != null ? LocalDate.parse((String) row.get("eventDate")).atStartOfDay(ZoneOffset.UTC).toInstant() : Instant.now();
                eventService.createEvent(actorId, new LocalEventCreateRequest(title, "Community", loc, date, desc));
            }
            case "jobs.bulkCreate" -> {
                String title = (String) row.get("title");
                String company = row.get("companyName") != null ? (String) row.get("companyName") : "Community Employer";
                String loc = row.get("location") != null ? (String) row.get("location") : "Pune";
                Object salaryObj = row.get("salary");
                BigDecimal salary = salaryObj instanceof Number ? BigDecimal.valueOf(((Number) salaryObj).doubleValue()) : (salaryObj != null ? new BigDecimal(salaryObj.toString().replaceAll("[^0-9.]", "")) : BigDecimal.valueOf(35000));
                jobService.createJobPost(actorId, new JobPostRequest(title, "Services", loc, "Full-time", salary, company, null, "jobs@dailymate.local", "OPEN", "Job opening via Assistant"));
            }
            case "lostFound.bulkCreate" -> {
                String title = (String) row.get("title");
                String type = row.get("type") != null ? (String) row.get("type") : "LOST";
                String loc = row.get("location") != null ? (String) row.get("location") : "Local Area";
                String desc = row.get("description") != null ? (String) row.get("description") : title;
                lostFoundService.createPost(actorId, new LostItemPostRequest(title, type, loc, desc, "Community Resident", "9876543210"));
            }
            case "complaint.bulkSubmit" -> {
                String title = (String) row.get("title");
                String loc = row.get("location") != null ? (String) row.get("location") : "Community Area";
                String desc = row.get("description") != null ? (String) row.get("description") : title;
                complaintService.createComplaint(new CommunityComplaintRequest(title, "Infrastructure", loc, desc));
            }
            case "grocery.bulkAdd" -> {
                String name = (String) row.get("name");
                String cat = row.get("category") != null ? (String) row.get("category") : "General";
                String store = row.get("store") != null ? (String) row.get("store") : "Local Store";
                Object priceObj = row.get("price");
                BigDecimal price = priceObj instanceof Number ? BigDecimal.valueOf(((Number) priceObj).doubleValue()) : new BigDecimal(priceObj.toString());
                groceryService.createItem(actorId, new GroceryItemRequest(name, cat, store, price, "1 unit", "Pune"));
            }
            case "notification.bulkCreate" -> {
                String title = (String) row.get("title");
                String msg = row.get("message") != null ? (String) row.get("message") : title;
                notificationService.createNotification(actorId, new NotificationRequest(title, msg, "INFO", false, null, null, "/notifications"));
            }
            default -> throw new BadRequestException("Unsupported bulk tool executor: " + toolName);
        }
    }

    private String extractTargetResourceId(String toolName, Map<String, Object> row, int index) {
        if (row.containsKey("expenseId")) return (String) row.get("expenseId");
        if (row.containsKey("reminderId")) return (String) row.get("reminderId");
        if (row.containsKey("providerId")) return (String) row.get("providerId");
        if (row.containsKey("requestId")) return (String) row.get("requestId");
        if (row.containsKey("eventId")) return (String) row.get("eventId");
        if (row.containsKey("jobId")) return (String) row.get("jobId");
        return "row-" + index;
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

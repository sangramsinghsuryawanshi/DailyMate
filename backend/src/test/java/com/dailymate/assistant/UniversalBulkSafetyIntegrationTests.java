package com.dailymate.assistant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dailymate.assistant.dto.request.BulkConfirmRequest;
import com.dailymate.assistant.dto.request.BulkPreviewRequest;
import com.dailymate.assistant.security.AssistantRateLimiter;
import com.dailymate.auth.dto.request.RegisterRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UniversalBulkSafetyIntegrationTests {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private AssistantRateLimiter rateLimiter;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setup() {
        rateLimiter.reset();
    }

    private String registerAndGetToken(String email) throws Exception {
        String tokenBody = mvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RegisterRequest(email, "StrongPass123!", "Bulk", "Tester"))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(tokenBody).get("accessToken").asText();
    }

    @Test
    void bulkInvariant1_missingOrInvalidRowsFlaggedInPreviewWithoutBlindMutation() throws Exception {
        String token = registerAndGetToken("bulk-inv1@example.com");

        List<Map<String, Object>> rows = List.of(
                Map.of("category", "Food", "description", "Lunch", "amount", 120.0),
                Map.of("category", "", "description", "Missing category", "amount", 50.0), // invalid
                Map.of("category", "Travel", "description", "Missing amount") // invalid
        );

        mvc.perform(post("/api/v1/assistant/bulk/preview")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new BulkPreviewRequest("expense.bulkRecord", rows))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRows").value(3))
                .andExpect(jsonPath("$.validRows").value(1))
                .andExpect(jsonPath("$.invalidRows").value(2))
                .andExpect(jsonPath("$.status").value("PENDING"));

        // Verify zero records inserted
        mvc.perform(get("/api/v1/expenses")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void bulkInvariant2_adminRoleGatingDeniesNormalUser() throws Exception {
        String userToken = registerAndGetToken("normal-user-bulk@example.com");

        List<Map<String, Object>> rows = List.of(
                Map.of("providerIds", List.of("prov-1", "prov-2"), "targetStatus", "ACTIVE")
        );

        // User invoking ADMIN-scoped bulk tool -> 403 Forbidden
        mvc.perform(post("/api/v1/assistant/bulk/preview")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new BulkPreviewRequest("marketplace.adminBulkActivate", rows))))
                .andExpect(status().isForbidden());
    }

    @Test
    void bulkInvariant3_tenantOwnershipIsolationInBulkDelete() throws Exception {
        String tokenA = registerAndGetToken("user-a-bulk-del@example.com");
        String tokenB = registerAndGetToken("user-b-bulk-del@example.com");

        // User A creates 1 expense
        List<Map<String, Object>> createRowsA = List.of(
                Map.of("category", "Food", "description", "Khichadi", "amount", 50.0)
        );
        String previewBodyA = mvc.perform(post("/api/v1/assistant/bulk/preview")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new BulkPreviewRequest("expense.bulkRecord", createRowsA))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String execIdA = objectMapper.readTree(previewBodyA).get("bulkExecutionId").asText();
        String previewHashA = objectMapper.readTree(previewBodyA).get("previewHash").asText();

        mvc.perform(post("/api/v1/assistant/bulk/{id}/confirm", execIdA)
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new BulkConfirmRequest(previewHashA))))
                .andExpect(status().isOk());

        String expensesA = mvc.perform(get("/api/v1/expenses")
                        .header("Authorization", "Bearer " + tokenA))
                .andReturn().getResponse().getContentAsString();
        String expenseIdA = objectMapper.readTree(expensesA).get(0).get("id").asText();

        // User B attempts to bulk delete User A's expense
        List<Map<String, Object>> deleteRowsB = List.of(
                Map.of("expenseId", expenseIdA)
        );
        String previewBodyB = mvc.perform(post("/api/v1/assistant/bulk/preview")
                        .header("Authorization", "Bearer " + tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new BulkPreviewRequest("expense.bulkDelete", deleteRowsB))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String execIdB = objectMapper.readTree(previewBodyB).get("bulkExecutionId").asText();
        String previewHashB = objectMapper.readTree(previewBodyB).get("previewHash").asText();

        // Execution for User B fails on User A's record (COMPLETED_WITH_ERRORS / FAILED)
        mvc.perform(post("/api/v1/assistant/bulk/{id}/confirm", execIdB)
                        .header("Authorization", "Bearer " + tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new BulkConfirmRequest(previewHashB))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FAILED"))
                .andExpect(jsonPath("$.failedRows").value(1));

        // Verify User A's expense is intact
        mvc.perform(get("/api/v1/expenses")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void bulkInvariant4_previewAndConfirmationMandate() throws Exception {
        String token = registerAndGetToken("preview-mandate@example.com");

        List<Map<String, Object>> rows = List.of(
                Map.of("category", "Groceries", "description", "Milk & Eggs", "amount", 180.0),
                Map.of("category", "Utilities", "description", "Wifi Recharge", "amount", 800.0)
        );

        String previewBody = mvc.perform(post("/api/v1/assistant/bulk/preview")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new BulkPreviewRequest("expense.bulkRecord", rows))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.validRows").value(2))
                .andReturn().getResponse().getContentAsString();

        // Zero mutations before confirm
        mvc.perform(get("/api/v1/expenses")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        // Confirm
        String execId = objectMapper.readTree(previewBody).get("bulkExecutionId").asText();
        String previewHash = objectMapper.readTree(previewBody).get("previewHash").asText();

        mvc.perform(post("/api/v1/assistant/bulk/{id}/confirm", execId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new BulkConfirmRequest(previewHash))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.succeededRows").value(2));

        // Exactly 2 records in DB after confirm
        mvc.perform(get("/api/v1/expenses")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void bulkInvariant5_idempotencyReplayProducesZeroDuplicateMutations() throws Exception {
        String token = registerAndGetToken("bulk-idemp@example.com");

        List<Map<String, Object>> rows = List.of(
                Map.of("category", "Shopping", "description", "Shoes", "amount", 2500.0)
        );

        String previewBody = mvc.perform(post("/api/v1/assistant/bulk/preview")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new BulkPreviewRequest("expense.bulkRecord", rows))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String execId = objectMapper.readTree(previewBody).get("bulkExecutionId").asText();
        String previewHash = objectMapper.readTree(previewBody).get("previewHash").asText();

        // Confirm 1
        mvc.perform(post("/api/v1/assistant/bulk/{id}/confirm", execId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new BulkConfirmRequest(previewHash))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.succeededRows").value(1));

        // Replay Confirm 2 (identical bulkExecutionId)
        mvc.perform(post("/api/v1/assistant/bulk/{id}/confirm", execId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new BulkConfirmRequest(previewHash))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.succeededRows").value(1));

        // Exactly 1 expense record in DB
        mvc.perform(get("/api/v1/expenses")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void bulkInvariant6_serverBatchLimitsEnforced() throws Exception {
        String token = registerAndGetToken("bulk-limits@example.com");

        // Construct 501 rows (> MAX_BULK_ROWS of 500)
        List<Map<String, Object>> hugeBatch = new ArrayList<>();
        for (int i = 0; i < 501; i++) {
            hugeBatch.add(Map.of("category", "Food", "description", "Item " + i, "amount", 10.0));
        }

        mvc.perform(post("/api/v1/assistant/bulk/preview")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new BulkPreviewRequest("expense.bulkRecord", hugeBatch))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void bulkInvariant7_partialFailureTruthfulness() throws Exception {
        String token = registerAndGetToken("partial-truth@example.com");

        List<Map<String, Object>> mixedRows = List.of(
                Map.of("category", "Food", "description", "Valid Lunch", "amount", 150.0),
                Map.of("category", "NonExistentCategory", "description", "Will Fail Validation")
        );

        String previewBody = mvc.perform(post("/api/v1/assistant/bulk/preview")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new BulkPreviewRequest("expense.bulkRecord", mixedRows))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String execId = objectMapper.readTree(previewBody).get("bulkExecutionId").asText();
        String previewHash = objectMapper.readTree(previewBody).get("previewHash").asText();

        mvc.perform(post("/api/v1/assistant/bulk/{id}/confirm", execId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new BulkConfirmRequest(previewHash))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED_WITH_ERRORS"))
                .andExpect(jsonPath("$.succeededRows").value(1))
                .andExpect(jsonPath("$.failedRows").value(1));
    }

    @Test
    void bulkInvariant8_previewHashIntegrityRejectsStaleExecution() throws Exception {
        String token = registerAndGetToken("stale-hash@example.com");

        List<Map<String, Object>> rows = List.of(
                Map.of("category", "Health", "description", "Vitamins", "amount", 350.0)
        );

        String previewBody = mvc.perform(post("/api/v1/assistant/bulk/preview")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new BulkPreviewRequest("expense.bulkRecord", rows))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String execId = objectMapper.readTree(previewBody).get("bulkExecutionId").asText();

        // Altered / invalid previewHash
        mvc.perform(post("/api/v1/assistant/bulk/{id}/confirm", execId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new BulkConfirmRequest("tampered-hash-12345"))))
                .andExpect(status().isConflict());
    }

    @Test
    void bulkInvariant9_rowFailureIsolationAccuratelyIdentifiesFailedRow() throws Exception {
        String token = registerAndGetToken("failure-isolation@example.com");

        List<Map<String, Object>> rows = List.of(
                Map.of("category", "Food", "description", "Snack 1", "amount", 40.0),
                Map.of("category", "InvalidCategoryWithoutAmount", "description", "Broken"),
                Map.of("category", "Food", "description", "Snack 2", "amount", 60.0)
        );

        String previewBody = mvc.perform(post("/api/v1/assistant/bulk/preview")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new BulkPreviewRequest("expense.bulkRecord", rows))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.validationErrors[0]").value(org.hamcrest.Matchers.containsString("Row 2")))
                .andReturn().getResponse().getContentAsString();

        String execId = objectMapper.readTree(previewBody).get("bulkExecutionId").asText();
        String previewHash = objectMapper.readTree(previewBody).get("previewHash").asText();

        mvc.perform(post("/api/v1/assistant/bulk/{id}/confirm", execId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new BulkConfirmRequest(previewHash))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED_WITH_ERRORS"))
                .andExpect(jsonPath("$.succeededRows").value(2))
                .andExpect(jsonPath("$.failedRows").value(1))
                .andExpect(jsonPath("$.failedRowDetails[0]").value(org.hamcrest.Matchers.containsString("Row 2")));
    }
}

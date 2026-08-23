package com.dailymate.assistant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dailymate.assistant.dto.request.BulkConfirmRequest;
import com.dailymate.assistant.dto.request.CanonicalBulkRequest;
import com.dailymate.assistant.entity.AssistantBulkOperation;
import com.dailymate.assistant.repository.AssistantBulkOperationRepository;
import com.dailymate.assistant.security.AssistantRateLimiter;
import com.dailymate.assistant.tool.AssistantToolDefinition;
import com.dailymate.assistant.tool.AssistantToolRegistry;
import com.dailymate.assistant.tool.DataSensitivity;
import com.dailymate.assistant.tool.OperationScope;
import com.dailymate.assistant.tool.ResourceVisibility;
import com.dailymate.assistant.tool.TargetSelectionMode;
import com.dailymate.assistant.tool.ToolOperationType;
import com.dailymate.assistant.tool.ToolRiskTier;
import com.dailymate.assistant.tool.ToolScope;
import com.dailymate.auth.dto.request.RegisterRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
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
class UniversalDynamicAiGovernanceTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private AssistantToolRegistry registry;

    @Autowired
    private AssistantBulkOperationRepository bulkRepo;

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
                        .content(objectMapper.writeValueAsString(new RegisterRequest(email, "StrongPass123!", "Governance", "Tester"))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(tokenBody).get("accessToken").asText();
    }

    @Test
    void dynamicRegistryPolicyValidator_assertsContractsOnAllTools() {
        Map<String, AssistantToolDefinition> tools = registry.getAllTools();
        assertThat(tools).isNotEmpty();

        for (AssistantToolDefinition tool : tools.values()) {
            // Invariant: READ operations must be Tier 1 and non-destructive
            if (tool.operationType() == ToolOperationType.READ || tool.operationType() == ToolOperationType.REPORT) {
                assertThat(tool.riskTier()).isEqualTo(ToolRiskTier.TIER_1);
                assertThat(tool.confirmationRequired()).isFalse();
                assertThat(tool.destructive()).isFalse();
            }

            // Invariant: Destructive operations must be Tier 3, confirmationRequired, auditRequired
            if (tool.destructive()) {
                assertThat(tool.riskTier()).isEqualTo(ToolRiskTier.TIER_3);
                assertThat(tool.confirmationRequired()).isTrue();
                assertThat(tool.auditRequired()).isTrue();
            }

            // Invariant: Bulk mutations must require preview and idempotency
            if (tool.operationScope() == OperationScope.BULK && tool.operationType() != ToolOperationType.READ) {
                assertThat(tool.bulkAllowed()).isTrue();
                assertThat(tool.previewRequired()).isTrue();
                assertThat(tool.idempotencyRequired()).isTrue();
            }

            // Invariant: User-private resources require ownership enforcement
            if (tool.scope() == ToolScope.USER && tool.visibility() == ResourceVisibility.USER_PRIVATE) {
                assertThat(tool.ownershipRequired()).isTrue();
            }

            // Invariant: Public searches do not mandate resource ownership
            if (tool.visibility() == ResourceVisibility.PUBLIC && tool.operationType() == ToolOperationType.READ) {
                assertThat(tool.ownershipRequired()).isFalse();
            }

            // Invariant: Admin tools must restrict roles to ADMIN and mandate adminReason for mutations
            if (tool.scope() == ToolScope.ADMIN) {
                assertThat(tool.allowedRoles()).containsExactly("ADMIN");
                if (tool.operationType() != ToolOperationType.READ) {
                    assertThat(tool.adminReasonRequired()).isTrue();
                }
                // Sensitive medical/financial records cannot be targeted by admin tools
                assertThat(tool.visibility()).isNotEqualTo(ResourceVisibility.USER_PRIVATE);
            }
        }
    }

    @Test
    void invariant21_filterPrivilegeIsolationBlocksCrossTenantFilter() throws Exception {
        String token = registerAndGetToken("filter-isolation@example.com");

        List<Map<String, Object>> rows = List.of(
                Map.of("category", "Food", "description", "Lunch", "amount", 100.0),
                Map.of("category", "Travel", "description", "Taxi", "amount", 200.0, "userId", "foreign-user-id-999") // cross-tenant attempt
        );

        CanonicalBulkRequest request = new CanonicalBulkRequest(
                "expense.bulkRecord",
                TargetSelectionMode.BY_FILTER,
                Map.of(),
                rows,
                null,
                false
        );

        mvc.perform(post("/api/v1/assistant/bulk/preview")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRows").value(2))
                .andExpect(jsonPath("$.validRows").value(1))
                .andExpect(jsonPath("$.invalidRows").value(1))
                .andExpect(jsonPath("$.validationErrors[0]").value(org.hamcrest.Matchers.containsString("cross-tenant")));
    }

    @Test
    void invariant22_dryRunModeReturnsPreviewWithoutPersistingPendingProposal() throws Exception {
        String token = registerAndGetToken("dry-run-tester@example.com");

        List<Map<String, Object>> rows = List.of(
                Map.of("category", "Food", "description", "Dry Run Snack", "amount", 50.0)
        );

        CanonicalBulkRequest request = new CanonicalBulkRequest(
                "expense.bulkRecord",
                TargetSelectionMode.BY_IMPORT,
                Map.of(),
                rows,
                null,
                true // dryRun
        );

        mvc.perform(post("/api/v1/assistant/bulk/preview")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dryRun").value(true))
                .andExpect(jsonPath("$.status").value("PREVIEW"));

        // Verify zero proposals saved in PENDING state
        assertThat(bulkRepo.findAll().stream().anyMatch(op -> op.getSummary().contains("Dry Run Snack"))).isFalse();
    }

    @Test
    void invariant23_confirmationPhraseIntegrityEnforcedOnDestructiveBulk() throws Exception {
        String token = registerAndGetToken("phrase-tester@example.com");

        List<Map<String, Object>> rows = List.of(
                Map.of("expenseId", "exp-sample-1"),
                Map.of("expenseId", "exp-sample-2")
        );

        CanonicalBulkRequest request = new CanonicalBulkRequest(
                "expense.bulkDelete",
                TargetSelectionMode.BY_IDS,
                Map.of(),
                rows,
                null,
                false
        );

        String previewBody = mvc.perform(post("/api/v1/assistant/bulk/preview")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.confirmationPhrase").value("CONFIRM DELETE 2 EXPENSE"))
                .andReturn().getResponse().getContentAsString();

        String execId = objectMapper.readTree(previewBody).get("bulkExecutionId").asText();
        String previewHash = objectMapper.readTree(previewBody).get("previewHash").asText();

        // Attempt confirm with wrong phrase -> 400 Bad Request
        mvc.perform(post("/api/v1/assistant/bulk/{id}/confirm", execId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new BulkConfirmRequest(previewHash, "WRONG PHRASE"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void invariant24_proposalExpirationBlocksStaleExecution() throws Exception {
        String token = registerAndGetToken("expiry-tester@example.com");

        List<Map<String, Object>> rows = List.of(
                Map.of("category", "Food", "description", "Expired Lunch", "amount", 90.0)
        );

        CanonicalBulkRequest request = new CanonicalBulkRequest(
                "expense.bulkRecord",
                TargetSelectionMode.BY_IMPORT,
                Map.of(),
                rows,
                null,
                false
        );

        String previewBody = mvc.perform(post("/api/v1/assistant/bulk/preview")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String execId = objectMapper.readTree(previewBody).get("bulkExecutionId").asText();
        String previewHash = objectMapper.readTree(previewBody).get("previewHash").asText();

        // Simulate TTL expiration in DB
        AssistantBulkOperation op = bulkRepo.findByBulkExecutionId(execId).orElseThrow();
        op.setExpiresAt(Instant.now().minus(1, ChronoUnit.MINUTES));
        bulkRepo.save(op);

        // Attempt confirm on expired proposal -> 409 Conflict
        mvc.perform(post("/api/v1/assistant/bulk/{id}/confirm", execId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new BulkConfirmRequest(previewHash, null))))
                .andExpect(status().isConflict());
    }
}

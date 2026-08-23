package com.dailymate.assistant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dailymate.assistant.dto.request.AssistantActionExecutionRequest;
import com.dailymate.assistant.dto.request.AssistantChatRequest;
import com.dailymate.assistant.dto.request.CanonicalBulkRequest;
import com.dailymate.assistant.tool.TargetSelectionMode;
import com.dailymate.assistant.repository.AssistantActionRepository;
import com.dailymate.assistant.security.AssistantRateLimiter;
import com.dailymate.auth.dto.request.RegisterRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * End-to-End AI Acceptance & Production Hardening Test Suite.
 * Covers user interaction, domain isolation, database mutations, idempotency replay,
 * cancellation state machines, and role authorization.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AssistantEndToEndAcceptanceIntegrationTests {

    @TestConfiguration
    static class FixedClockTestConfig {
        @Bean
        @Primary
        public Clock fixedClock() {
            // Anchor test clock to Sunday, August 23, 2026 at 12:00:00 IST (+05:30)
            return Clock.fixed(Instant.parse("2026-08-23T06:30:00Z"), ZoneId.of("Asia/Kolkata"));
        }
    }

    @Autowired
    private MockMvc mvc;

    @Autowired
    private AssistantActionRepository actionRepository;

    @Autowired
    private AssistantRateLimiter rateLimiter;

    @Autowired
    private Clock clock;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setup() {
        rateLimiter.reset();
    }

    private String registerAndGetToken(String email) throws Exception {
        String tokenBody = mvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RegisterRequest(email, "SecureE2EPass123!", "E2E", "User"))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(tokenBody).get("accessToken").asText();
    }

    @Test
    @DisplayName("Test 1: Exact Original 5-Expense Failure -> AI Orchestration -> Preview -> Zero DB Mutation -> Confirm -> 5 Records (₹7,279.00)")
    void testScenario1_ExactFiveExpenseOriginalFailure_ExecutionAndDBMutation() throws Exception {
        String token = registerAndGetToken("e2e-scenario1@example.com");

        String prompt = """
                Add these 5 expenses to my account:
                1. Groceries ₹1850 today
                2. Fuel ₹1200 yesterday
                3. Internet ₹999 on August 17
                4. Electricity ₹2450 on August 19
                5. Restaurant ₹780 on August 16
                """;

        // 1. AI Request & Tool Selection
        String chatRes = mvc.perform(post("/api/v1/assistant/chat")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AssistantChatRequest(prompt))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.proposedAction").exists())
                .andExpect(jsonPath("$.proposedAction.actionType").value("BULK_RECORD_EXPENSES"))
                .andExpect(jsonPath("$.proposedAction.summary").value(Matchers.containsString("₹7,279.00")))
                .andExpect(jsonPath("$.proposedAction.summary").value(Matchers.containsString("5 expenses")))
                .andReturn().getResponse().getContentAsString();

        JsonNode chatJson = objectMapper.readTree(chatRes);
        String actionId = chatJson.get("proposedAction").get("actionId").asText();

        // 2. Invariant: ZERO database mutations occur before confirmation
        mvc.perform(get("/api/v1/expenses")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.content.length()").value(0));

        // 3. User Confirms Action
        mvc.perform(post("/api/v1/assistant/actions/{id}/confirm", actionId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AssistantActionExecutionRequest("e2e-exp-key-1"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EXECUTED"))
                .andExpect(jsonPath("$.resultMessage").value(Matchers.containsString("₹7,279.00")));

        // 4. Invariant: Exactly 5 new records inserted with exact fields, dates, and ownership
        String expensesJson = mvc.perform(get("/api/v1/expenses")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(5))
                .andExpect(jsonPath("$.content.length()").value(5))
                .andReturn().getResponse().getContentAsString();

        JsonNode expenses = objectMapper.readTree(expensesJson).get("content");
        double totalSum = 0.0;
        for (JsonNode exp : expenses) {
            totalSum += exp.get("amount").asDouble();
        }
        assertThat(totalSum).isEqualTo(7279.00);

        // 5. Idempotent Replay Verification: Repeat confirmation produces ZERO additional database records
        mvc.perform(post("/api/v1/assistant/actions/{id}/confirm", actionId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AssistantActionExecutionRequest("e2e-exp-key-1"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EXECUTED"));

        mvc.perform(get("/api/v1/expenses")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(5));
    }

    @Test
    @DisplayName("Test 2: Multi-Domain Isolation (Expenses + Medicine Reminders + Grocery Items) -> Strict Anti-Contamination -> Preview -> DB Zero Mutation")
    void testScenario2_MultiDomainIsolation_ExpensesMedicineGroceries() throws Exception {
        String token = registerAndGetToken("e2e-scenario2@example.com");

        String multiDomainPrompt = """
                I want to add the following to DailyMate:

                Expenses:
                - Groceries ₹1,850
                - Fuel ₹1,200

                Medicine reminders:
                - Vitamin D at 8:00 AM daily
                - Vitamin B12 at 9:00 AM daily

                Grocery items:
                - Rice 5 kg
                - Milk 1 litre

                Create a bulk operation preview containing all three domains.
                Do not execute anything until I explicitly confirm.
                """;

        // 1. Send Multi-Domain Prompt
        String chatRes = mvc.perform(post("/api/v1/assistant/chat")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AssistantChatRequest(multiDomainPrompt))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.proposedAction").doesNotExist()) // Proposal strictly NULL until domain selection
                .andExpect(jsonPath("$.response").value(Matchers.containsString("Expenses")))
                .andExpect(jsonPath("$.response").value(Matchers.containsString("2 items")))
                .andExpect(jsonPath("$.response").value(Matchers.containsString("Medicine Reminders")))
                .andExpect(jsonPath("$.response").value(Matchers.containsString("Groceries")))
                .andReturn().getResponse().getContentAsString();

        // 2. Verify Anti-Cross-Contamination: zero records in database across all three domains
        mvc.perform(get("/api/v1/expenses").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));

        mvc.perform(get("/api/v1/medicine-reminders").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));

        mvc.perform(get("/api/v1/grocery/items").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));

        // 3. User selects domain: "Process expenses"
        String convoId = objectMapper.readTree(chatRes).get("id").asText();
        String selectRes = mvc.perform(post("/api/v1/assistant/chat")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AssistantChatRequest("Process expenses", convoId))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.proposedAction").exists())
                .andExpect(jsonPath("$.proposedAction.actionType").value("BULK_RECORD_EXPENSES"))
                .andExpect(jsonPath("$.proposedAction.summary").value(Matchers.containsString("₹3,050.00")))
                .andReturn().getResponse().getContentAsString();

        String actionId = objectMapper.readTree(selectRes).get("proposedAction").get("actionId").asText();

        // 4. Confirm expenses execution
        mvc.perform(post("/api/v1/assistant/actions/{id}/confirm", actionId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AssistantActionExecutionRequest("e2e-multi-exp"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EXECUTED"));

        // 5. Verify DB: Exactly 2 expenses, 0 medicines, 0 groceries
        mvc.perform(get("/api/v1/expenses").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));

        mvc.perform(get("/api/v1/medicine-reminders").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));

        mvc.perform(get("/api/v1/grocery/items").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    @DisplayName("Test 3: Notification Isolation -> Bulk System Notifications -> Zero Expense Contamination")
    void testScenario3_NotificationIsolation_BulkSystemNotifications() throws Exception {
        String token = registerAndGetToken("e2e-scenario3@example.com");

        String notificationPrompt = """
                Notifications:
                - Welcome to DailyMate: Your DailyMate account is ready.
                - Expense Reminder: Remember to review today's expenses.
                - New Community Event: A new community event is available near you.
                """;

        String chatRes = mvc.perform(post("/api/v1/assistant/chat")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AssistantChatRequest(notificationPrompt))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.proposedAction").exists())
                .andExpect(jsonPath("$.proposedAction.actionType").value("BULK_CREATE_NOTIFICATIONS"))
                .andExpect(jsonPath("$.proposedAction.actionType").value(Matchers.not("RECORD_EXPENSE")))
                .andExpect(jsonPath("$.proposedAction.actionType").value(Matchers.not("BULK_RECORD_EXPENSES")))
                .andReturn().getResponse().getContentAsString();

        String actionId = objectMapper.readTree(chatRes).get("proposedAction").get("actionId").asText();

        // Confirm
        mvc.perform(post("/api/v1/assistant/actions/{id}/confirm", actionId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AssistantActionExecutionRequest("e2e-notif-key"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EXECUTED"));

        // Verify Notifications created, Zero expenses created
        mvc.perform(get("/api/v1/notifications").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3));

        mvc.perform(get("/api/v1/expenses").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    @DisplayName("Test 4: Unsupported Request -> Flight Booking -> Friendly Refusal -> No Tool Fallback & Zero DB Mutations")
    void testScenario4_UnsupportedRequest_FlightBooking() throws Exception {
        String token = registerAndGetToken("e2e-scenario4@example.com");

        mvc.perform(post("/api/v1/assistant/chat")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AssistantChatRequest("Book me a flight from Pune to Delhi tomorrow."))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.proposedAction").doesNotExist())
                .andExpect(jsonPath("$.response").value(Matchers.containsString("DailyMate does not support direct third-party bookings")));

        // Zero mutations across database
        mvc.perform(get("/api/v1/expenses").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    @DisplayName("Test 5: Confirmation Replay Idempotency -> Double Execution Replay Protection")
    void testScenario5_ConfirmationReplay_Idempotency() throws Exception {
        String token = registerAndGetToken("e2e-scenario5@example.com");

        String chatRes = mvc.perform(post("/api/v1/assistant/chat")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AssistantChatRequest("Add expense for Coffee 40"))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String actionId = objectMapper.readTree(chatRes).get("proposedAction").get("actionId").asText();

        // 1. Confirm 1 with Key
        mvc.perform(post("/api/v1/assistant/actions/{id}/confirm", actionId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AssistantActionExecutionRequest("e2e-replay-key-100"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EXECUTED"));

        // 2. Confirm 2 (Replay with SAME key) -> 200 OK with cached result
        mvc.perform(post("/api/v1/assistant/actions/{id}/confirm", actionId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AssistantActionExecutionRequest("e2e-replay-key-100"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EXECUTED"));

        // 3. Confirm 3 (Attempt with DIFFERENT key) -> 409 Conflict
        mvc.perform(post("/api/v1/assistant/actions/{id}/confirm", actionId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AssistantActionExecutionRequest("different-key-300"))))
                .andExpect(status().isConflict());

        // 4. Exact count in database: 1 expense record
        mvc.perform(get("/api/v1/expenses").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].amount").value(40.00))
                .andExpect(jsonPath("$.content[0].description").value("Coffee"));
    }

    @Test
    @DisplayName("Test 6: Cancellation State Machine -> Prevents Subsequent Execution & Ensures Zero DB Records")
    void testScenario6_Cancellation_PreventsSubsequentExecution() throws Exception {
        String token = registerAndGetToken("e2e-scenario6@example.com");

        String chatRes = mvc.perform(post("/api/v1/assistant/chat")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AssistantChatRequest("Add expense for Gym 1500"))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String actionId = objectMapper.readTree(chatRes).get("proposedAction").get("actionId").asText();

        // 1. Cancel action
        mvc.perform(post("/api/v1/assistant/actions/{id}/cancel", actionId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        // 2. Verify Zero Records in Database
        mvc.perform(get("/api/v1/expenses").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));

        // 3. Attempt Execution on Cancelled Action -> 400 Bad Request
        mvc.perform(post("/api/v1/assistant/actions/{id}/confirm", actionId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AssistantActionExecutionRequest("key-after-cancel"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(Matchers.containsString("cancelled")));

        // 4. Database still has 0 records
        mvc.perform(get("/api/v1/expenses").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    @DisplayName("Test 7: Admin Tool RBAC -> Standard User Blocked with 403 Forbidden")
    void testScenario7_AdminToolRBAC_StandardUserBlocked() throws Exception {
        String userToken = registerAndGetToken("e2e-standard-user@example.com");

        List<Map<String, Object>> rows = List.of(
                Map.of("userIds", List.of("user-1", "user-2"), "status", "LOCKED", "reason", "Security audit")
        );

        // Standard user invoking ADMIN tool -> 403 Forbidden
        mvc.perform(post("/api/v1/assistant/bulk/preview")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CanonicalBulkRequest(
                                "admin.bulkUserStatusUpdate",
                                TargetSelectionMode.BY_IDS,
                                Map.of(),
                                rows,
                                "Security suspension",
                                false))))
                .andExpect(status().isForbidden());
    }
}

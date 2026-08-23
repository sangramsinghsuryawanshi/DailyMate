package com.dailymate.assistant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dailymate.assistant.dto.request.AssistantActionExecutionRequest;
import com.dailymate.assistant.dto.request.AssistantChatRequest;
import com.dailymate.assistant.repository.AssistantActionRepository;
import com.dailymate.assistant.security.AssistantRateLimiter;
import com.dailymate.auth.dto.request.RegisterRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
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

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AssistantToolCallingHarnessIntegrationTests {

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
                        .content(objectMapper.writeValueAsString(new RegisterRequest(email, "SecureHarnessPass123!", "Tool", "Harness"))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(tokenBody).get("accessToken").asText();
    }

    @Test
    @DisplayName("Exact Regression: 5-Expense List with ₹1850, ₹1200, ₹999, ₹2450, ₹780 totaling ₹7,279.00 and Relative Dates")
    void testExactFiveExpenseParsingAndRelativeDates() throws Exception {
        String token = registerAndGetToken("harness-5expenses@example.com");

        String prompt = """
                Add these 5 expenses to my account:
                1. Groceries ₹1850 today
                2. Electricity bill ₹1200 yesterday
                3. Medical prescription ₹999 on August 17
                4. Fuel ₹2450 on last Wednesday
                5. Dinner with family ₹780 on last Sunday
                """;

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

        // Verify parameters payload inside proposedAction
        String paramsStr = actionJsonParams(actionId);
        JsonNode paramsJson = objectMapper.readTree(paramsStr);
        assertThat(paramsJson.isArray()).isTrue();
        assertThat(paramsJson.size()).isEqualTo(5);

        // 1. Groceries ₹1850 today (2026-08-23)
        JsonNode item1 = paramsJson.get(0);
        assertThat(item1.get("amount").asDouble()).isEqualTo(1850.00);
        assertThat(item1.get("description").asText()).isEqualTo("Groceries");
        assertThat(item1.get("spentOn").asText()).isEqualTo("2026-08-23");

        // 2. Electricity bill ₹1200 yesterday (2026-08-22)
        JsonNode item2 = paramsJson.get(1);
        assertThat(item2.get("amount").asDouble()).isEqualTo(1200.00);
        assertThat(item2.get("description").asText()).isEqualTo("Electricity bill");
        assertThat(item2.get("spentOn").asText()).isEqualTo("2026-08-22");

        // 3. Medical prescription ₹999 on August 17 (2026-08-17)
        JsonNode item3 = paramsJson.get(2);
        assertThat(item3.get("amount").asDouble()).isEqualTo(999.00);
        assertThat(item3.get("description").asText()).isEqualTo("Medical prescription");
        assertThat(item3.get("spentOn").asText()).isEqualTo("2026-08-17");

        // 4. Fuel ₹2450 on last Wednesday (2026-08-19)
        JsonNode item4 = paramsJson.get(3);
        assertThat(item4.get("amount").asDouble()).isEqualTo(2450.00);
        assertThat(item4.get("description").asText()).isEqualTo("Fuel");
        assertThat(item4.get("spentOn").asText()).isEqualTo("2026-08-19");

        // 5. Dinner with family ₹780 on last Sunday (2026-08-16)
        JsonNode item5 = paramsJson.get(4);
        assertThat(item5.get("amount").asDouble()).isEqualTo(780.00);
        assertThat(item5.get("description").asText()).isEqualTo("Dinner with family");
        assertThat(item5.get("spentOn").asText()).isEqualTo("2026-08-16");

        // Confirm Action Execution
        mvc.perform(post("/api/v1/assistant/actions/{id}/confirm", actionId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AssistantActionExecutionRequest("key-5expenses"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EXECUTED"))
                .andExpect(jsonPath("$.resultMessage").value(Matchers.containsString("₹7,279.00")));

        // Verify that exactly 5 expenses are stored in the database
        mvc.perform(get("/api/v1/expenses")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(5))
                .andExpect(jsonPath("$.content.length()").value(5));
    }

    @Test
    @DisplayName("Anti-Fallback Guard: Unknown domain operations must never create expenses")
    void testAntiFallbackRuleForUnknownDomains() throws Exception {
        String token = registerAndGetToken("harness-antifallback@example.com");

        // Requesting flight ticket booking
        mvc.perform(post("/api/v1/assistant/chat")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AssistantChatRequest("Book flight tickets to Mumbai for ₹5000"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.proposedAction").doesNotExist())
                .andExpect(jsonPath("$.response").value(Matchers.containsString("DailyMate does not support direct third-party bookings")));

        // Requesting money transfer
        mvc.perform(post("/api/v1/assistant/chat")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AssistantChatRequest("Transfer ₹2000 via UPI to Sangram"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.proposedAction").doesNotExist())
                .andExpect(jsonPath("$.response").value(Matchers.containsString("DailyMate does not support direct third-party bookings or financial money transfers")));
    }

    @Test
    @DisplayName("Cross-Domain Contamination Guard: Notifications and Groceries must not become expenses or medicines")
    void testCrossDomainContaminationGuards() throws Exception {
        String token = registerAndGetToken("harness-purity@example.com");

        // Notification must NOT become an expense
        mvc.perform(post("/api/v1/assistant/chat")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AssistantChatRequest("Add notification to call doctor at 5 PM"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.proposedAction.actionType").value("CREATE_NOTIFICATION"))
                .andExpect(jsonPath("$.proposedAction.actionType").value(Matchers.not("RECORD_EXPENSE")));

        // Medicine reminder must NOT become an expense
        mvc.perform(post("/api/v1/assistant/chat")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AssistantChatRequest("Schedule medicine reminder for Metformin 500mg at 9:00 PM"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.proposedAction.actionType").value("CREATE_REMINDER"))
                .andExpect(jsonPath("$.proposedAction.actionType").value(Matchers.not("RECORD_EXPENSE")));
    }

    @Test
    @DisplayName("Multi-Domain Payload: Preview Breakdown and Interactive Disambiguation")
    void testMultiDomainPayloadIsolation() throws Exception {
        String token = registerAndGetToken("harness-multidomain@example.com");

        String multiDomainPrompt = """
                Expenses:
                - Lunch ₹150
                - Coffee ₹50

                Medicines:
                - Paracetamol 500mg at 08:00 AM

                Emergency Contacts:
                - Dr. Sharma (Doctor): 9876543210
                """;

        String res = mvc.perform(post("/api/v1/assistant/chat")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AssistantChatRequest(multiDomainPrompt))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.proposedAction").doesNotExist()) // Invariant: Proposal is strictly NULL until single domain selected
                .andExpect(jsonPath("$.response").value(Matchers.containsString("Expenses")))
                .andExpect(jsonPath("$.response").value(Matchers.containsString("Medicine Reminders")))
                .andExpect(jsonPath("$.response").value(Matchers.containsString("Emergency Contacts")))
                .andReturn().getResponse().getContentAsString();

        // Follow up with selection
        mvc.perform(post("/api/v1/assistant/chat")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AssistantChatRequest("Process expenses", "conv-multi-123"))))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("Confirmation State Machine & Database-Level Idempotency Replay")
    void testActionConfirmationIdempotency() throws Exception {
        String token = registerAndGetToken("harness-idempotency@example.com");

        // 1. Propose Action
        String chatRes = mvc.perform(post("/api/v1/assistant/chat")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AssistantChatRequest("Add expense for snacks amount 120"))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String actionId = objectMapper.readTree(chatRes).get("proposedAction").get("actionId").asText();

        // 2. First Execution with Key
        mvc.perform(post("/api/v1/assistant/actions/{id}/confirm", actionId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AssistantActionExecutionRequest("idem-key-100"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EXECUTED"));

        // 3. Idempotent Replay with Same Key -> 200 OK with Replay
        mvc.perform(post("/api/v1/assistant/actions/{id}/confirm", actionId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AssistantActionExecutionRequest("idem-key-100"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EXECUTED"));

        // 4. Second Execution Attempt with Different Key -> 409 Conflict
        mvc.perform(post("/api/v1/assistant/actions/{id}/confirm", actionId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AssistantActionExecutionRequest("different-key-200"))))
                .andExpect(status().isConflict());

        // 5. Verify Only ONE Expense Was Created
        mvc.perform(get("/api/v1/expenses")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].amount").value(120.00));
    }

    private String actionJsonParams(String actionId) {
        return actionRepository.findById(actionId)
                .map(a -> a.getParametersJson())
                .orElse("{}");
    }
}

package com.dailymate.assistant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dailymate.assistant.dto.request.AssistantActionExecutionRequest;
import com.dailymate.assistant.dto.request.AssistantChatRequest;
import com.dailymate.assistant.entity.AssistantAction;
import com.dailymate.assistant.repository.AssistantActionRepository;
import com.dailymate.assistant.security.AssistantRateLimiter;
import com.dailymate.auth.dto.request.RegisterRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
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
class AssistantToolExecutionIntegrationTests {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private AssistantActionRepository actionRepository;

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
                        .content(objectMapper.writeValueAsString(new RegisterRequest(email, "StrongPass123!", "Tool", "Tester"))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(tokenBody).get("accessToken").asText();
    }

    @Test
    void naturalLanguageExpenseExtraction_khichadi50() throws Exception {
        String token = registerAndGetToken("nl-khichadi@example.com");

        // "add expense for my afternoon lunch name khichadi and amount is 50"
        String chatBody = mvc.perform(post("/api/v1/assistant/chat")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AssistantChatRequest("add expense for my afternoon lunch name khichadi and amount is 50"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.proposedAction").exists())
                .andExpect(jsonPath("$.proposedAction.actionType").value("RECORD_EXPENSE"))
                .andExpect(jsonPath("$.proposedAction.summary").value(org.hamcrest.Matchers.containsString("₹50.00")))
                .andExpect(jsonPath("$.proposedAction.summary").value(org.hamcrest.Matchers.containsString("Khichadi")))
                .andReturn().getResponse().getContentAsString();

        JsonNode res = objectMapper.readTree(chatBody);
        String actionId = res.get("proposedAction").get("actionId").asText();

        // Confirm
        mvc.perform(post("/api/v1/assistant/actions/{id}/confirm", actionId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AssistantActionExecutionRequest("key-khichadi"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EXECUTED"))
                .andExpect(jsonPath("$.resultMessage").value(org.hamcrest.Matchers.containsString("₹50.00")))
                .andExpect(jsonPath("$.resultMessage").value(org.hamcrest.Matchers.containsString("Khichadi")));

        // Verify domain expense table has amount 50.00 and description Khichadi (NOT 500 or Groceries)
        mvc.perform(get("/api/v1/expenses")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].amount").value(50.00))
                .andExpect(jsonPath("$[0].description").value("Khichadi"))
                .andExpect(jsonPath("$[0].category").value("Food & Dining"));
    }

    @Test
    void naturalLanguageExpenseExtraction_missingAmountAsksUser() throws Exception {
        String token = registerAndGetToken("missing-amount@example.com");

        // "Add expense for khichadi" (no amount given) -> No action proposal, asks amount
        mvc.perform(post("/api/v1/assistant/chat")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AssistantChatRequest("Add expense for khichadi"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.proposedAction").doesNotExist())
                .andExpect(jsonPath("$.response").value(org.hamcrest.Matchers.containsString("What amount should I record for Khichadi?")));
    }

    @Test
    void naturalLanguageExpenseExtraction_missingDescriptionAsksUser() throws Exception {
        String token = registerAndGetToken("missing-desc@example.com");

        // "Add expense of 50" (no description given) -> No action proposal, asks what it is for
        mvc.perform(post("/api/v1/assistant/chat")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AssistantChatRequest("Add expense of 50"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.proposedAction").doesNotExist())
                .andExpect(jsonPath("$.response").value(org.hamcrest.Matchers.containsString("What is this expense of ₹50.00 for?")));
    }

    @Test
    void conversationContinuity_preservesSameConversationOnMultiTurn() throws Exception {
        String token = registerAndGetToken("continuity@example.com");

        // Message 1 (New Conversation)
        String chat1 = mvc.perform(post("/api/v1/assistant/chat")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AssistantChatRequest("Hello DailyMate"))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String convoId = objectMapper.readTree(chat1).get("id").asText();

        // Message 2 (Same Conversation using conversationId)
        String chat2 = mvc.perform(post("/api/v1/assistant/chat")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AssistantChatRequest("How much have I spent?", convoId))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String convoId2 = objectMapper.readTree(chat2).get("id").asText();

        // Convo ID must remain identical
        assertThat(convoId2).isEqualTo(convoId);

        // Sidebar conversations must contain exactly 1 conversation
        mvc.perform(get("/api/v1/assistant/conversations")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void proposesAndExecutesExpenseCreation() throws Exception {
        String token = registerAndGetToken("tool-expense@example.com");

        // 1. Propose action via chat
        String chatBody = mvc.perform(post("/api/v1/assistant/chat")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AssistantChatRequest("Add expense 750 for Utilities"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.proposedAction").exists())
                .andExpect(jsonPath("$.proposedAction.actionType").value("RECORD_EXPENSE"))
                .andExpect(jsonPath("$.proposedAction.status").value("PENDING"))
                .andReturn().getResponse().getContentAsString();

        JsonNode chatRes = objectMapper.readTree(chatBody);
        String actionId = chatRes.get("proposedAction").get("actionId").asText();

        // 2. Confirm action with idempotency key
        mvc.perform(post("/api/v1/assistant/actions/{id}/confirm", actionId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AssistantActionExecutionRequest("key-expense-1"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EXECUTED"))
                .andExpect(jsonPath("$.resultMessage").value(org.hamcrest.Matchers.containsString("₹750.00")));

        // 3. Verify domain service created the actual expense record
        mvc.perform(get("/api/v1/expenses")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].category").value("Utilities"))
                .andExpect(jsonPath("$[0].amount").value(750.00));
    }

    @Test
    void proposesAndExecutesMedicineReminderCreation() throws Exception {
        String token = registerAndGetToken("tool-reminder@example.com");

        // 1. Propose action via chat
        String chatBody = mvc.perform(post("/api/v1/assistant/chat")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AssistantChatRequest("Set reminder for Amoxicillin 250mg at 14:00"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.proposedAction").exists())
                .andExpect(jsonPath("$.proposedAction.actionType").value("CREATE_REMINDER"))
                .andReturn().getResponse().getContentAsString();

        String actionId = objectMapper.readTree(chatBody).get("proposedAction").get("actionId").asText();

        // 2. Confirm action
        mvc.perform(post("/api/v1/assistant/actions/{id}/confirm", actionId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EXECUTED"))
                .andExpect(jsonPath("$.resultMessage").value(org.hamcrest.Matchers.containsString("Amoxicillin")));

        // 3. Verify medicine reminder exists in domain repository
        mvc.perform(get("/api/v1/medicine-reminders")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Amoxicillin"))
                .andExpect(jsonPath("$[0].dosage").value("250mg"));
    }

    @Test
    void idempotencyReplaysSuccessForSameKeyAndRejectsDifferentKey() throws Exception {
        String token = registerAndGetToken("idempotency-test@example.com");

        // Propose expense
        String chatBody = mvc.perform(post("/api/v1/assistant/chat")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AssistantChatRequest("Add expense 350 for Groceries"))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String actionId = objectMapper.readTree(chatBody).get("proposedAction").get("actionId").asText();

        // 1. First execution with key-A -> 200 EXECUTED
        mvc.perform(post("/api/v1/assistant/actions/{id}/confirm", actionId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AssistantActionExecutionRequest("key-A"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EXECUTED"));

        // 2. Replay with SAME key-A -> returns 200 EXECUTED with identical result
        mvc.perform(post("/api/v1/assistant/actions/{id}/confirm", actionId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AssistantActionExecutionRequest("key-A"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EXECUTED"));

        // 3. Attempt execution with DIFFERENT key-B -> 409 Conflict
        mvc.perform(post("/api/v1/assistant/actions/{id}/confirm", actionId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AssistantActionExecutionRequest("key-B"))))
                .andExpect(status().isConflict());

        // 4. Verify exactly ONE expense was created in database
        mvc.perform(get("/api/v1/expenses")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void allowsCancellationAndPreventsExecutionOfCancelledAction() throws Exception {
        String token = registerAndGetToken("cancel-test@example.com");

        // Propose expense
        String chatBody = mvc.perform(post("/api/v1/assistant/chat")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AssistantChatRequest("Add expense 500 for Travel"))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String actionId = objectMapper.readTree(chatBody).get("proposedAction").get("actionId").asText();

        // 1. Cancel action -> returns CANCELLED
        mvc.perform(post("/api/v1/assistant/actions/{id}/cancel", actionId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        // 2. Attempt to confirm cancelled action -> 400 Bad Request
        mvc.perform(post("/api/v1/assistant/actions/{id}/confirm", actionId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("cancelled")));
    }

    @Test
    void enforcesCrossTenantIsolationOnActionsWith404() throws Exception {
        String tokenA = registerAndGetToken("user-a-tools@example.com");
        String tokenB = registerAndGetToken("user-b-tools@example.com");

        // User A proposes action
        String chatBody = mvc.perform(post("/api/v1/assistant/chat")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AssistantChatRequest("Add expense 1200 for Health"))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String actionId = objectMapper.readTree(chatBody).get("proposedAction").get("actionId").asText();

        // User B attempts to confirm User A's action -> 404 Not Found
        mvc.perform(post("/api/v1/assistant/actions/{id}/confirm", actionId)
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound());

        // User B attempts to cancel User A's action -> 404 Not Found
        mvc.perform(post("/api/v1/assistant/actions/{id}/cancel", actionId)
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound());
    }

    @Test
    void preventsExecutionOfExpiredAction() throws Exception {
        String token = registerAndGetToken("expired-test@example.com");

        // Propose action
        String chatBody = mvc.perform(post("/api/v1/assistant/chat")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AssistantChatRequest("Add expense 200 for Other"))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String actionId = objectMapper.readTree(chatBody).get("proposedAction").get("actionId").asText();

        // Expire action in database
        AssistantAction action = actionRepository.findById(actionId).orElseThrow();
        action.setExpiresAt(Instant.now().minus(1, ChronoUnit.HOURS));
        actionRepository.save(action);

        // Attempt confirm -> 400 Bad Request
        mvc.perform(post("/api/v1/assistant/actions/{id}/confirm", actionId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("expired")));
    }

    @Test
    void handlesConcurrentConfirmationsSafely() throws Exception {
        String token = registerAndGetToken("concurrent-tools@example.com");

        // Propose expense
        String chatBody = mvc.perform(post("/api/v1/assistant/chat")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AssistantChatRequest("Add expense 600 for Groceries"))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String actionId = objectMapper.readTree(chatBody).get("proposedAction").get("actionId").asText();

        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            List<Callable<Integer>> tasks = List.of(
                    () -> mvc.perform(post("/api/v1/assistant/actions/{id}/confirm", actionId)
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new AssistantActionExecutionRequest("concurrent-key"))))
                            .andReturn().getResponse().getStatus(),
                    () -> mvc.perform(post("/api/v1/assistant/actions/{id}/confirm", actionId)
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new AssistantActionExecutionRequest("concurrent-key"))))
                            .andReturn().getResponse().getStatus()
            );

            List<Future<Integer>> results = executor.invokeAll(tasks);
            for (Future<Integer> res : results) {
                assertThat(res.get()).isEqualTo(200);
            }

            // Exactly 1 expense record created in database
            mvc.perform(get("/api/v1/expenses")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].amount").value(600.00));
        } finally {
            executor.shutdown();
        }
    }
}

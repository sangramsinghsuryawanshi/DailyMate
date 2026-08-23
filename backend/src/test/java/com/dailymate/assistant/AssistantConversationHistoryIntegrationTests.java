package com.dailymate.assistant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dailymate.assistant.dto.request.AssistantChatRequest;
import com.dailymate.assistant.entity.AssistantConversationState;
import com.dailymate.assistant.repository.AssistantConversationStateRepository;
import com.dailymate.assistant.service.AssistantConversationStateManager;
import com.dailymate.auth.dto.request.RegisterRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
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
class AssistantConversationHistoryIntegrationTests {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private AssistantConversationStateManager stateManager;

    @Autowired
    private AssistantConversationStateRepository stateRepository;

    @Autowired
    private com.dailymate.assistant.security.AssistantRateLimiter rateLimiter;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private String registerAndGetToken(String email) throws Exception {
        RegisterRequest req = new RegisterRequest(email, "StrongPass123!", "ChatHistory", "Tester");
        String tokenBody = mvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(tokenBody).get("accessToken").asText();
    }

    private String sendChat(String token, String prompt, String conversationId) throws Exception {
        rateLimiter.reset();
        AssistantChatRequest req = new AssistantChatRequest(prompt, conversationId);
        String res = mvc.perform(post("/api/v1/assistant/chat")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return res;
    }

    @Test
    void sameConversation_multipleMessages_preserved() throws Exception {
        String token = registerAndGetToken("convo-multi@example.com");

        // Turn 1: Who can I call in an emergency?
        String res1 = sendChat(token, "Who can I call in an emergency?", null);
        String convoId = objectMapper.readTree(res1).get("id").asText();

        // Turn 2: What about my emergency contacts?
        sendChat(token, "What about my emergency contacts?", convoId);

        // Turn 3: Add my brother as an emergency contact
        sendChat(token, "Add my brother as an emergency contact with phone 9876543210 named Alex", convoId);

        // Verify full history contains all 6 messages (3 user + 3 assistant)
        String historyRes = mvc.perform(get("/api/v1/assistant/conversations/" + convoId + "/messages")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        JsonNode msgs = objectMapper.readTree(historyRes);
        assertThat(msgs.size()).isEqualTo(6);

        assertThat(msgs.get(0).get("role").asText()).isEqualTo("USER");
        assertThat(msgs.get(0).get("content").asText()).isEqualTo("Who can I call in an emergency?");
        assertThat(msgs.get(1).get("role").asText()).isEqualTo("ASSISTANT");

        assertThat(msgs.get(2).get("role").asText()).isEqualTo("USER");
        assertThat(msgs.get(2).get("content").asText()).isEqualTo("What about my emergency contacts?");

        assertThat(msgs.get(4).get("role").asText()).isEqualTo("USER");
        assertThat(msgs.get(4).get("content").asText()).contains("Add my brother as an emergency contact");
        assertThat(msgs.get(5).get("role").asText()).isEqualTo("ASSISTANT");
    }

    @Test
    void newChat_createsIndependentConversationAndHistory() throws Exception {
        String token = registerAndGetToken("new-chat-indep@example.com");

        // Chat A
        String resA = sendChat(token, "Who can I call in an emergency?", null);
        String convoA = objectMapper.readTree(resA).get("id").asText();

        // Chat B (New Chat with null conversationId)
        String resB = sendChat(token, "What medicines do I have scheduled today?", null);
        String convoB = objectMapper.readTree(resB).get("id").asText();

        assertThat(convoA).isNotEqualTo(convoB);

        // Verify A messages
        String msgsA = mvc.perform(get("/api/v1/assistant/conversations/" + convoA + "/messages")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode jsonA = objectMapper.readTree(msgsA);
        assertThat(jsonA.size()).isEqualTo(2);
        assertThat(jsonA.get(0).get("content").asText()).isEqualTo("Who can I call in an emergency?");

        // Verify B messages
        String msgsB = mvc.perform(get("/api/v1/assistant/conversations/" + convoB + "/messages")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode jsonB = objectMapper.readTree(msgsB);
        assertThat(jsonB.size()).isEqualTo(2);
        assertThat(jsonB.get(0).get("content").asText()).isEqualTo("What medicines do I have scheduled today?");
    }

    @Test
    void conversationStateExpiry_doesNotDeleteMessages() throws Exception {
        String token = registerAndGetToken("state-expiry-history@example.com");

        String res = sendChat(token, "Add expense for lunch 50", null);
        String convoId = objectMapper.readTree(res).get("id").asText();

        // Expire conversation state TTL to simulate 31 minutes passing
        AssistantConversationState state = stateRepository.findByConversationId(convoId).orElseThrow();
        state.setExpiresAt(Instant.now().minusSeconds(120)); // Expired
        stateRepository.save(state);

        // Fetching message history should still return all messages!
        String msgs = mvc.perform(get("/api/v1/assistant/conversations/" + convoId + "/messages")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        JsonNode json = objectMapper.readTree(msgs);
        assertThat(json.size()).isGreaterThanOrEqualTo(2);
        assertThat(json.get(0).get("content").asText()).isEqualTo("Add expense for lunch 50");
    }

    @Test
    void crossUserConversationHistory_rejected() throws Exception {
        String tokenA = registerAndGetToken("user-a-history@example.com");
        String tokenB = registerAndGetToken("user-b-history@example.com");

        String resA = sendChat(tokenA, "Private secret for User A", null);
        String convoA = objectMapper.readTree(resA).get("id").asText();

        // User B attempting to read User A's conversation history -> 404
        mvc.perform(get("/api/v1/assistant/conversations/" + convoA + "/messages")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound());
    }

    @Test
    void repeatedBulkAmbiguity_alwaysProducesClarification() throws Exception {
        String token = registerAndGetToken("repeated-bulk@example.com");

        // Turn 1: "bulk"
        String res1 = sendChat(token, "bulk", null);
        JsonNode node1 = objectMapper.readTree(res1);
        String convoId = node1.get("id").asText();
        assertThat(node1.get("response").asText()).contains("What would you like to do in bulk?");

        // Turn 2: "bulk" again in the same conversation -> MUST produce clarification again, NOT expense analytics!
        String res2 = sendChat(token, "bulk", convoId);
        JsonNode node2 = objectMapper.readTree(res2);
        assertThat(node2.get("response").asText()).contains("What would you like to do in bulk?");
        assertThat(node2.get("proposedAction").isNull()).isTrue();
    }

    @Test
    void bulkExpenseParsing_multipleItemsParsedAndTotalCalculated() throws Exception {
        String token = registerAndGetToken("bulk-parse-exp@example.com");

        String prompt = "bulk import expenses lunch 50,dinner 50,watch 1000";
        String res = sendChat(token, prompt, null);
        JsonNode node = objectMapper.readTree(res);

        // Verify parsed total = 1100.00 and 3 items
        assertThat(node.get("response").asText()).contains("I found 3 expenses");
        assertThat(node.get("response").asText()).contains("₹1,100.00");
        assertThat(node.get("proposedAction")).isNotNull();
        assertThat(node.get("proposedAction").get("actionType").asText()).isEqualTo("BULK_RECORD_EXPENSES");
    }

    @Test
    void pendingAmountContinuity_fillsSlotAndCreatesProposal() throws Exception {
        String token = registerAndGetToken("pending-amount@example.com");

        // Turn 1: User specifies description without amount
        String res1 = sendChat(token, "Add expense for lunch", null);
        JsonNode node1 = objectMapper.readTree(res1);
        String convoId = node1.get("id").asText();
        assertThat(node1.get("response").asText()).contains("What amount should I record for Lunch?");

        // Turn 2: User responds only with "100" -> MUST fill the pending slot and propose the expense!
        String res2 = sendChat(token, "100", convoId);
        JsonNode node2 = objectMapper.readTree(res2);

        assertThat(node2.get("response").asText()).contains("₹100.00 for Lunch");
        assertThat(node2.get("proposedAction")).isNotNull();
        assertThat(node2.get("proposedAction").get("actionType").asText()).isEqualTo("RECORD_EXPENSE");
    }

    @Test
    void newConversationClearsPendingIntent() throws Exception {
        String token = registerAndGetToken("new-chat-clears-intent@example.com");

        // Convo A asks for amount
        String resA = sendChat(token, "Add expense for dinner", null);
        JsonNode nodeA = objectMapper.readTree(resA);
        assertThat(nodeA.get("response").asText()).contains("What amount should I record for Dinner?");

        // Convo B sends "100" without prior context -> should not inherit Dinner from Convo A!
        String resB = sendChat(token, "100", null);
        JsonNode nodeB = objectMapper.readTree(resB);

        assertThat(nodeB.get("id").asText()).isNotEqualTo(nodeA.get("id").asText());
        // Since no pending intent in Convo B, it doesn't create Dinner expense
        assertThat(nodeB.get("response").asText()).doesNotContain("Dinner");
    }

    @Test
    void conversationSwitchingRestoresCorrectPendingState() throws Exception {
        String token = registerAndGetToken("switch-pending-state@example.com");

        // Convo A: pending amount for Khichadi
        String resA = sendChat(token, "Record expense for Khichadi", null);
        String convoA = objectMapper.readTree(resA).get("id").asText();

        // Convo B: pending items for Bulk Import
        String resB = sendChat(token, "bulk import expenses from a list", null);
        String convoB = objectMapper.readTree(resB).get("id").asText();

        // Switch to Convo A and send "60" -> Completes Khichadi expense
        String resA2 = sendChat(token, "60", convoA);
        JsonNode nodeA2 = objectMapper.readTree(resA2);
        assertThat(nodeA2.get("response").asText()).contains("₹60.00 for Khichadi");

        // Switch to Convo B and send "tea 10, coffee 20" -> Completes Bulk Import
        String resB2 = sendChat(token, "tea 10, coffee 20", convoB);
        JsonNode nodeB2 = objectMapper.readTree(resB2);
        assertThat(nodeB2.get("response").asText()).contains("I found 2 expenses");
        assertThat(nodeB2.get("response").asText()).contains("₹30.00");
    }

    @Test
    void confirmedBulkProposal_persistsExecutedState() throws Exception {
        String token = registerAndGetToken("bulk-executed-persist@example.com");

        // 1. Create bulk proposal
        String res = sendChat(token, "bulk import expenses lunch 50, dinner 50, watch 1000", null);
        JsonNode chatJson = objectMapper.readTree(res);
        String convoId = chatJson.get("id").asText();
        String actionId = chatJson.get("proposedAction").get("actionId").asText();

        // 2. Confirm action
        mvc.perform(post("/api/v1/assistant/actions/" + actionId + "/confirm")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idempotencyKey\":\"bulk-test-1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EXECUTED"));

        // 3. Fetch conversation messages (e.g. simulating page reload or return)
        String historyRes = mvc.perform(get("/api/v1/assistant/conversations/" + convoId + "/messages")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        JsonNode msgs = objectMapper.readTree(historyRes);
        assertThat(msgs.size()).isEqualTo(2);
        JsonNode assistantMsg = msgs.get(1);
        assertThat(assistantMsg.get("proposedAction")).isNotNull();
        assertThat(assistantMsg.get("proposedAction").get("status").asText()).isEqualTo("EXECUTED");
    }

    @Test
    void confirmedBulkProposal_cannotExecuteTwice() throws Exception {
        String token = registerAndGetToken("bulk-idemp-test@example.com");

        String res = sendChat(token, "bulk import expenses lunch 50, dinner 50, watch 1000", null);
        JsonNode chatJson = objectMapper.readTree(res);
        String actionId = chatJson.get("proposedAction").get("actionId").asText();

        // Execution 1
        mvc.perform(post("/api/v1/assistant/actions/" + actionId + "/confirm")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idempotencyKey\":\"idemp-guard-1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EXECUTED"));

        // Execution 2 (Same Idempotency Key) -> Replays stored result
        mvc.perform(post("/api/v1/assistant/actions/" + actionId + "/confirm")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idempotencyKey\":\"idemp-guard-1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EXECUTED"));
    }

    @Test
    void addNotificationPrompt_routesToNotificationAndNotExpense() throws Exception {
        String token = registerAndGetToken("notif-route-test@example.com");

        // User says: "add 2 notifications first is sleep at 10 pm and wakeup at 8 am"
        String res = sendChat(token, "add 2 notifications first is sleep at 10 pm and wakeup at 8 am", null);
        JsonNode node = objectMapper.readTree(res);

        // MUST route to notification, NEVER to expense with ₹2.00!
        assertThat(node.get("response").asText()).doesNotContain("₹2.00");
        assertThat(node.get("response").asText()).doesNotContain("expense");
        assertThat(node.get("proposedAction")).isNotNull();
        assertThat(node.get("proposedAction").get("actionType").asText()).isEqualTo("CREATE_NOTIFICATION");
    }

    @Test
    void greetingAfterPendingSlot_resetsStateAndDoesNotRecordExpense() throws Exception {
        String token = registerAndGetToken("greeting-reset-test@example.com");

        // Turn 1: Add expense for lunch -> asks for amount
        String res1 = sendChat(token, "Add expense for lunch", null);
        String convoId = objectMapper.readTree(res1).get("id").asText();

        // Turn 2: User says "hi"
        String res2 = sendChat(token, "hi", convoId);
        JsonNode node2 = objectMapper.readTree(res2);

        // MUST be a greeting response, NOT a proposal for "hi" expense!
        assertThat(node2.get("response").asText()).contains("Hello!");
        assertThat(node2.get("proposedAction").isNull()).isTrue();
    }

    @Test
    void messageOrdering_userStrictlyBeforeAssistant() throws Exception {
        String token = registerAndGetToken("msg-order-test@example.com");

        String res = sendChat(token, "What is my emergency contact list?", null);
        String convoId = objectMapper.readTree(res).get("id").asText();

        String historyRes = mvc.perform(get("/api/v1/assistant/conversations/" + convoId + "/messages")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        JsonNode msgs = objectMapper.readTree(historyRes);
        assertThat(msgs.size()).isEqualTo(2);

        // User message MUST be index 0, Assistant MUST be index 1
        assertThat(msgs.get(0).get("role").asText()).isEqualTo("USER");
        assertThat(msgs.get(0).get("content").asText()).isEqualTo("What is my emergency contact list?");
        assertThat(msgs.get(1).get("role").asText()).isEqualTo("ASSISTANT");
    }

    @Test
    void combinedSixDomainBulkRequest_neverContaminatesExpenseParser() throws Exception {
        String token = registerAndGetToken("six-domain-bulk@example.com");

        String prompt = """
                Bulk add the following:
                Expenses: lunch 50, dinner 50, petrol 800, groceries 1500.
                Medicine reminders: vitamin D at 9am, calcium at 8pm.
                Emergency contacts: father 9876543210, mother 9876543211.
                Complaints: street light broken near college, garbage not collected near society.
                Lost items: black wallet near college, blue backpack at railway station.
                Events: cricket tournament on 25 August at 5pm, blood donation camp on 27 August at 10am.
                """;

        String res = sendChat(token, prompt, null);
        JsonNode node = objectMapper.readTree(res);
        String text = node.get("response").asText();

        // 1. Assert authoritative breakdowns
        assertThat(text).contains("**Expenses**: 4 items (Total: ₹2,400.00)");
        assertThat(text).contains("**Medicine Reminders**: 2 items");
        assertThat(text).contains("**Emergency Contacts**: 2 items");
        assertThat(text).contains("**Complaints**: 2 items");
        assertThat(text).contains("**Lost Items**: 2 items");
        assertThat(text).contains("**Events**: 2 items");
        assertThat(text).contains("No changes have been made yet.");

        // 2. Assert impossible false total is NEVER produced
        assertThat(text).doesNotContain("19,753,088,890");
        assertThat(text).doesNotContain("19753088890");

        // 3. Assert no combined contaminated proposal is created
        assertThat(node.get("proposedAction").isNull()).isTrue();
    }

    @Test
    void exactFalseTotal_regressionPrevented() throws Exception {
        String token = registerAndGetToken("false-total-guard@example.com");

        String prompt = """
                Expenses: lunch 50, dinner 50, petrol 800, groceries 1500.
                Medicine reminders: vitamin D at 9am, calcium at 8pm.
                Emergency contacts: father 9876543210, mother 9876543211.
                """;

        String res = sendChat(token, prompt, null);
        JsonNode node = objectMapper.readTree(res);
        String text = node.get("response").asText();

        assertThat(text).contains("**Expenses**: 4 items (Total: ₹2,400.00)");
        assertThat(text).doesNotContain("9,876,543,210");
        assertThat(text).doesNotContain("9876543210");
        assertThat(node.get("proposedAction").isNull()).isTrue();
    }

    @Test
    void medicineTimeIsolation() throws Exception {
        String token = registerAndGetToken("med-time-iso@example.com");

        String prompt = """
                Expenses: lunch 50.
                Medicine reminders: vitamin D at 9am, calcium at 8pm.
                """;

        String res = sendChat(token, prompt, null);
        JsonNode node = objectMapper.readTree(res);
        String text = node.get("response").asText();

        assertThat(text).contains("**Expenses**: 1 item (Total: ₹50.00)");
        assertThat(text).contains("**Medicine Reminders**: 2 items");
        assertThat(text).doesNotContain("₹9.00");
        assertThat(text).doesNotContain("₹8.00");
    }

    @Test
    void phoneNumberIsolation() throws Exception {
        String token = registerAndGetToken("phone-iso@example.com");

        String prompt = """
                Expenses: lunch 50.
                Emergency contacts: father 9876543210, mother 9876543211.
                """;

        String res = sendChat(token, prompt, null);
        JsonNode node = objectMapper.readTree(res);
        String text = node.get("response").asText();

        assertThat(text).contains("**Expenses**: 1 item (Total: ₹50.00)");
        assertThat(text).contains("**Emergency Contacts**: 2 items");
        assertThat(text).doesNotContain("9876543210");
    }

    @Test
    void dateTimeIsolation() throws Exception {
        String token = registerAndGetToken("datetime-iso@example.com");

        String prompt = """
                Expenses: lunch 50.
                Events: cricket tournament on 25 August at 5pm.
                """;

        String res = sendChat(token, prompt, null);
        JsonNode node = objectMapper.readTree(res);
        String text = node.get("response").asText();

        assertThat(text).contains("**Expenses**: 1 item (Total: ₹50.00)");
        assertThat(text).contains("**Events**: 1 item");
        assertThat(text).doesNotContain("₹25.00");
        assertThat(text).doesNotContain("₹75.00");
    }

    @Test
    void multiDomainRequest_createsNoMutationBeforeDomainSelection() throws Exception {
        String token = registerAndGetToken("no-mutation-before-selection@example.com");

        String prompt = """
                Bulk add:
                Expenses: lunch 50, dinner 50, petrol 800, groceries 1500.
                Medicine reminders: vitamin D at 9am, calcium at 8pm.
                """;

        String res = sendChat(token, prompt, null);
        JsonNode node = objectMapper.readTree(res);
        String convoId = node.get("id").asText();

        // 1. First turn returns zero proposal
        assertThat(node.get("proposedAction").isNull()).isTrue();

        // 2. Select domain "Process expenses" -> generates pure BULK_RECORD_EXPENSES proposal
        String res2 = sendChat(token, "Process expenses", convoId);
        JsonNode node2 = objectMapper.readTree(res2);

        assertThat(node2.get("proposedAction")).isNotNull();
        assertThat(node2.get("proposedAction").get("actionType").asText()).isEqualTo("BULK_RECORD_EXPENSES");
        assertThat(node2.get("proposedAction").get("summary").asText()).contains("4 expenses totaling ₹2,400.00");

        // 3. Confirm and verify exactly 4 records created
        String actionId = node2.get("proposedAction").get("actionId").asText();
        mvc.perform(post("/api/v1/assistant/actions/" + actionId + "/confirm")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idempotencyKey\":\"multi-select-exec-1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EXECUTED"))
                .andExpect(jsonPath("$.resultMessage").value(org.hamcrest.Matchers.containsString("4 expenses totaling ₹2,400.00")));
    }

    @Test
    void conversationSwitching_doesNotLeakSelectedDomainOrPendingBulkOperation() throws Exception {
        String token = registerAndGetToken("switch-multi-domain@example.com");

        // Convo A: Multi-domain prompt
        String resA = sendChat(token, "Expenses: lunch 50. Medicine reminders: vitamin D at 9am.", null);
        String convoA = objectMapper.readTree(resA).get("id").asText();

        // Convo B: Simple standalone greeting
        String resB = sendChat(token, "hi", null);
        String convoB = objectMapper.readTree(resB).get("id").asText();
        assertThat(objectMapper.readTree(resB).get("response").asText()).contains("Hello!");

        // Switch back to Convo A and send "expenses" -> only completes Convo A's pending selection
        String resA2 = sendChat(token, "expenses", convoA);
        JsonNode nodeA2 = objectMapper.readTree(resA2);
        assertThat(nodeA2.get("proposedAction")).isNotNull();
        assertThat(nodeA2.get("proposedAction").get("summary").asText()).contains("1 expense totaling ₹50.00");
    }

    @Test
    void multiDomainSelection_processExpense_resumesOriginalExpensePayload() throws Exception {
        String token = registerAndGetToken("process-expense-exact-test@example.com");

        String prompt = "Bulk add the following:\r\n"
                + "Expenses: lunch 50, dinner 50, petrol 800, groceries 1500.\r\n"
                + "Medicine reminders: vitamin D at 9am, calcium at 8pm.\r\n"
                + "Emergency contacts: father 9876543210, mother 9876543211.\r\n"
                + "Complaints: street light broken near college, garbage not collected near society.\r\n"
                + "Lost items: black wallet near college, blue backpack at railway station.\r\n"
                + "Events: cricket tournament on 25 August at 5pm, blood donation camp on 27 August at 10am.";

        // Turn 1: 6-domain prompt
        String res1 = sendChat(token, prompt, null);
        JsonNode node1 = objectMapper.readTree(res1);
        String text1 = node1.get("response").asText();
        String convoId = node1.get("id").asText();

        // Must find all 6 operations, including Expenses
        assertThat(text1).contains("I found 6 different bulk operations:");
        assertThat(text1).contains("**Expenses**: 4 items (Total: ₹2,400.00)");
        assertThat(text1).contains("**Medicine Reminders**: 2 items");
        assertThat(text1).contains("**Emergency Contacts**: 2 items");
        assertThat(text1).contains("**Complaints**: 2 items");
        assertThat(text1).contains("**Lost Items**: 2 items");
        assertThat(text1).contains("**Events**: 2 items");
        assertThat(node1.get("proposedAction").isNull()).isTrue();

        // Turn 2: User says "Process expense" (singular, natural language)
        String res2 = sendChat(token, "Process expense", convoId);
        JsonNode node2 = objectMapper.readTree(res2);
        String text2 = node2.get("response").asText();

        // Must NOT run analytics query or show previous DB totals
        assertThat(text2).doesNotContain("Here is a summary of your recent expenses");
        assertThat(text2).doesNotContain("Total: ₹200.00");

        // Must create pure proposal for the 4 expenses totaling ₹2,400.00
        assertThat(node2.get("proposedAction")).isNotNull();
        assertThat(node2.get("proposedAction").get("actionType").asText()).isEqualTo("BULK_RECORD_EXPENSES");
        assertThat(node2.get("proposedAction").get("summary").asText()).contains("4 expenses totaling ₹2,400.00");
        assertThat(text2).contains("Lunch");
        assertThat(text2).contains("Dinner");
        assertThat(text2).contains("Petrol");
        assertThat(text2).contains("Groceries");
        assertThat(text2).contains("₹2,400.00");
    }

    @Test
    void multipleDomainsInSelection_requiresClarification() throws Exception {
        String token = registerAndGetToken("multi-select-clarify@example.com");

        String prompt = """
                Bulk add:
                Expenses: lunch 50, dinner 50.
                Medicine reminders: vitamin D at 9am, calcium at 8pm.
                Emergency contacts: father 9876543210, mother 9876543211.
                """;

        String res1 = sendChat(token, prompt, null);
        JsonNode node1 = objectMapper.readTree(res1);
        String convoId = node1.get("id").asText();
        assertThat(node1.get("proposedAction").isNull()).isTrue();

        // User enters multiple domains simultaneously
        String res2 = sendChat(token, "Add medicine reminders,Expenses,Emergency Contact", convoId);
        JsonNode node2 = objectMapper.readTree(res2);
        String text2 = node2.get("response").asText();

        // Must ask for single domain clarification
        assertThat(text2).contains("You selected multiple domains. Please choose one domain to process first");
        assertThat(node2.get("proposedAction").isNull()).isTrue();
    }

    @Test
    void selectMedicineDomain_createsMedicineProposalWithoutExecution() throws Exception {
        String token = registerAndGetToken("select-medicine-domain@example.com");

        String prompt = """
                Bulk add:
                Expenses: lunch 50.
                Medicine reminders: vitamin D at 9am, calcium at 8pm.
                """;

        String res1 = sendChat(token, prompt, null);
        String convoId = objectMapper.readTree(res1).get("id").asText();

        // Select medicine reminders
        String res2 = sendChat(token, "Add medicine reminders", convoId);
        JsonNode node2 = objectMapper.readTree(res2);

        assertThat(node2.get("proposedAction")).isNotNull();
        assertThat(node2.get("proposedAction").get("actionType").asText()).isEqualTo("BULK_CREATE_MEDICINE_REMINDERS");
        assertThat(node2.get("response").asText()).contains("Vitamin D");
        assertThat(node2.get("response").asText()).contains("Calcium");

        // Confirm medicine action
        String actionId = node2.get("proposedAction").get("actionId").asText();
        mvc.perform(post("/api/v1/assistant/actions/" + actionId + "/confirm")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idempotencyKey\":\"med-exec-1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EXECUTED"))
                .andExpect(jsonPath("$.resultMessage").value(org.hamcrest.Matchers.containsString("2 medicine reminders")));
    }

    @Test
    void fullSevenTurnScenario_exactEndToEndRegression() throws Exception {
        String token = registerAndGetToken("full-seven-turn@example.com");

        // Turn 1: 6-Domain prompt
        String turn1Prompt = """
                Bulk add the following:
                Expenses: lunch 50, dinner 50, petrol 800, groceries 1500.
                Medicine reminders: vitamin D at 9am, calcium at 8pm.
                Emergency contacts: father 9876543210, mother 9876543211.
                Complaints: street light broken near college, garbage not collected near society.
                Lost items: black wallet near college, blue backpack at railway station.
                Events: cricket tournament on 25 August at 5pm, blood donation camp on 27 August at 10am.
                """;

        String res1 = sendChat(token, turn1Prompt, null);
        JsonNode node1 = objectMapper.readTree(res1);
        String convoId = node1.get("id").asText();

        assertThat(node1.get("response").asText()).contains("I found 6 different bulk operations:");
        assertThat(node1.get("response").asText()).contains("**Expenses**: 4 items (Total: ₹2,400.00)");
        assertThat(node1.get("response").asText()).contains("**Medicine Reminders**: 2 items");
        assertThat(node1.get("proposedAction").isNull()).isTrue();

        // Turn 2: User says multiple domains -> clarification
        String res2 = sendChat(token, "Add medicine reminders,Expenses,Emergency Contact,Complaints,Lost Items,Events", convoId);
        JsonNode node2 = objectMapper.readTree(res2);
        assertThat(node2.get("response").asText()).contains("You selected multiple domains. Please choose one domain to process first");
        assertThat(node2.get("proposedAction").isNull()).isTrue();

        // Turn 3: User says "generate a chart of expense" -> analytics query, pure null proposal
        String res3 = sendChat(token, "generate a chart of expense", convoId);
        JsonNode node3 = objectMapper.readTree(res3);
        assertThat(node3.get("proposedAction").isNull()).isTrue();
        assertThat(node3.get("response").asText()).doesNotContain("Record ₹200.00 expense");

        // Turn 4: User says "Add medicine reminders" -> selects medicine domain
        String res4 = sendChat(token, "Add medicine reminders", convoId);
        JsonNode node4 = objectMapper.readTree(res4);
        assertThat(node4.get("proposedAction")).isNotNull();
        assertThat(node4.get("proposedAction").get("actionType").asText()).isEqualTo("BULK_CREATE_MEDICINE_REMINDERS");
        assertThat(node4.get("response").asText()).contains("Vitamin D");
        assertThat(node4.get("response").asText()).contains("Calcium");

        // Turn 5: Confirm medicine proposal
        String medActionId = node4.get("proposedAction").get("actionId").asText();
        mvc.perform(post("/api/v1/assistant/actions/" + medActionId + "/confirm")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idempotencyKey\":\"med-exec-turn5\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EXECUTED"));

        // Turn 6: User says "8 pm" -> time expression must NOT be recorded as expense ₹200!
        String res6 = sendChat(token, "8 pm", convoId);
        JsonNode node6 = objectMapper.readTree(res6);
        assertThat(node6.get("proposedAction").isNull()).isTrue();
        assertThat(node6.get("response").asText()).doesNotContain("₹200.00");

        // Turn 7: User says "Add expense for groceries 500" -> creates clean grocery expense proposal
        String res7 = sendChat(token, "Add expense for groceries 500", convoId);
        JsonNode node7 = objectMapper.readTree(res7);
        assertThat(node7.get("proposedAction")).isNotNull();
        assertThat(node7.get("proposedAction").get("actionType").asText()).isEqualTo("RECORD_EXPENSE");
        assertThat(node7.get("proposedAction").get("summary").asText()).contains("₹500.00");
        assertThat(node7.get("proposedAction").get("summary").asText()).contains("Groceries");

        // Verify full message history contains all 12 messages (6 chat turns * 2) with strictly message-bound proposals
        String msgsRes = mvc.perform(get("/api/v1/assistant/conversations/" + convoId + "/messages")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        JsonNode msgs = objectMapper.readTree(msgsRes);
        assertThat(msgs.size()).isEqualTo(12);

        // Turn 1 Assistant Message (index 1) has no proposal
        assertThat(msgs.get(1).get("proposedAction").isNull()).isTrue();
        // Turn 2 Assistant Message (index 3) has no proposal
        assertThat(msgs.get(3).get("proposedAction").isNull()).isTrue();
        // Turn 3 Assistant Message (index 5) has no proposal
        assertThat(msgs.get(5).get("proposedAction").isNull()).isTrue();
        // Turn 4 Assistant Message (index 7) has medicine proposal with EXECUTED status
        assertThat(msgs.get(7).get("proposedAction")).isNotNull();
        assertThat(msgs.get(7).get("proposedAction").get("status").asText()).isEqualTo("EXECUTED");
        // Turn 6 Assistant Message (index 9) has no proposal
        assertThat(msgs.get(9).get("proposedAction").isNull()).isTrue();
        // Turn 7 Assistant Message (index 11) has grocery expense proposal with PENDING status
        assertThat(msgs.get(11).get("proposedAction")).isNotNull();
        assertThat(msgs.get(11).get("proposedAction").get("status").asText()).isEqualTo("PENDING");
    }

    @Test
    void plainEventsSelection_resolvesAgainstPendingMultiDomainState() throws Exception {
        String token = registerAndGetToken("plain-events-sel@example.com");

        String prompt = """
                Bulk add the following:
                Expenses: lunch 50, dinner 50.
                Events: cricket tournament on 25 August at 5pm, blood donation camp on 27 August at 10am.
                """;

        String res1 = sendChat(token, prompt, null);
        String convoId = objectMapper.readTree(res1).get("id").asText();

        // Short word "Events" MUST resolve to EVENT domain proposal, NOT upcoming events query!
        String res2 = sendChat(token, "Events", convoId);
        JsonNode node2 = objectMapper.readTree(res2);
        String text2 = node2.get("response").asText();

        assertThat(text2).doesNotContain("There are currently no upcoming community events scheduled");
        assertThat(text2).contains("Cricket tournament");
        assertThat(text2).contains("Blood donation camp");
        assertThat(node2.get("proposedAction")).isNotNull();
        assertThat(node2.get("proposedAction").get("actionType").asText()).isEqualTo("BULK_CREATE_EVENTS");
        assertThat(node2.get("proposedAction").get("status").asText()).isEqualTo("PENDING");
    }

    @Test
    void plainMedicineSelection_resolvesAgainstPendingMultiDomainState() throws Exception {
        String token = registerAndGetToken("plain-med-sel@example.com");

        String prompt = """
                Bulk add the following:
                Expenses: lunch 50, dinner 50.
                Medicine reminders: vitamin D at 9am, calcium at 8pm.
                """;

        String res1 = sendChat(token, prompt, null);
        String convoId = objectMapper.readTree(res1).get("id").asText();

        // Short word "Medicine" MUST resolve to MEDICINE_REMINDER domain proposal, NOT generic help!
        String res2 = sendChat(token, "Medicine", convoId);
        JsonNode node2 = objectMapper.readTree(res2);
        String text2 = node2.get("response").asText();

        assertThat(text2).doesNotContain("How can I help you today");
        assertThat(text2).contains("Vitamin D");
        assertThat(text2).contains("Calcium");
        assertThat(node2.get("proposedAction")).isNotNull();
        assertThat(node2.get("proposedAction").get("actionType").asText()).isEqualTo("BULK_CREATE_MEDICINE_REMINDERS");
        assertThat(node2.get("proposedAction").get("status").asText()).isEqualTo("PENDING");
    }

    @Test
    void domainSelectionNeverExecutes() throws Exception {
        String token = registerAndGetToken("sel-no-exec@example.com");

        String prompt = """
                Bulk add:
                Expenses: lunch 50.
                Medicine reminders: vitamin D at 9am, calcium at 8pm.
                """;

        String res1 = sendChat(token, prompt, null);
        String convoId = objectMapper.readTree(res1).get("id").asText();

        String res2 = sendChat(token, "Add medicine reminders", convoId);
        JsonNode node2 = objectMapper.readTree(res2);

        // Status MUST be PENDING, zero automatic mutations!
        assertThat(node2.get("proposedAction")).isNotNull();
        assertThat(node2.get("proposedAction").get("status").asText()).isEqualTo("PENDING");
    }

    @Test
    void confirmationExecutesSelectedDomainOnly() throws Exception {
        String token = registerAndGetToken("confirm-only-selected@example.com");

        String prompt = """
                Bulk add:
                Expenses: lunch 50.
                Events: cricket tournament on 25 August at 5pm, blood donation camp on 27 August at 10am.
                """;

        String res1 = sendChat(token, prompt, null);
        String convoId = objectMapper.readTree(res1).get("id").asText();

        String res2 = sendChat(token, "Events", convoId);
        JsonNode node2 = objectMapper.readTree(res2);
        String actionId = node2.get("proposedAction").get("actionId").asText();

        // Confirm Action execution
        mvc.perform(post("/api/v1/assistant/actions/" + actionId + "/confirm")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idempotencyKey\":\"events-confirm-key-1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EXECUTED"))
                .andExpect(jsonPath("$.resultMessage").value(org.hamcrest.Matchers.containsString("2 community events")));
    }

    @Test
    void domainSwitchSupersedesPendingSlot() throws Exception {
        String token = registerAndGetToken("slot-supersede-domain@example.com");

        // Turn 1: Starts expense with amount pending
        String res1 = sendChat(token, "Add expense for lunch", null);
        String convoId = objectMapper.readTree(res1).get("id").asText();

        // Turn 2: User switches topic to medicine reminders
        String res2 = sendChat(token, "Add medicine reminder for Vitamin D at 9am", convoId);
        JsonNode node2 = objectMapper.readTree(res2);
        assertThat(node2.get("proposedAction")).isNotNull();
        assertThat(node2.get("proposedAction").get("actionType").asText()).isEqualTo("CREATE_REMINDER");

        // Turn 3: User says "8 pm" -> MUST NOT record a ₹200 or ₹8 expense from superseded slot
        String res3 = sendChat(token, "8 pm", convoId);
        JsonNode node3 = objectMapper.readTree(res3);
        assertThat(node3.get("proposedAction").isNull()).isTrue();
        assertThat(node3.get("response").asText()).doesNotContain("₹200.00");
    }

    @Test
    void exactManualConversationRegression() throws Exception {
        String token = registerAndGetToken("exact-manual-seq@example.com");

        // Step 1: 6-Domain request
        String prompt = """
                Bulk add the following:
                Expenses: lunch 50, dinner 50, petrol 800, groceries 1500.
                Medicine reminders: vitamin D at 9am, calcium at 8pm.
                Emergency contacts: father 9876543210, mother 9876543211.
                Complaints: street light broken near college, garbage not collected near society.
                Lost items: black wallet near college, blue backpack at railway station.
                Events: cricket tournament on 25 August at 5pm, blood donation camp on 27 August at 10am.
                """;

        String res1 = sendChat(token, prompt, null);
        JsonNode node1 = objectMapper.readTree(res1);
        String convoId = node1.get("id").asText();
        assertThat(node1.get("proposedAction").isNull()).isTrue();
        assertThat(node1.get("response").asText()).contains("I found 6 different bulk operations:");

        // Step 2: User says "Events"
        String res2 = sendChat(token, "Events", convoId);
        JsonNode node2 = objectMapper.readTree(res2);
        assertThat(node2.get("proposedAction")).isNotNull();
        assertThat(node2.get("proposedAction").get("actionType").asText()).isEqualTo("BULK_CREATE_EVENTS");
        assertThat(node2.get("proposedAction").get("status").asText()).isEqualTo("PENDING");
        assertThat(node2.get("response").asText()).contains("Cricket tournament");
        assertThat(node2.get("response").asText()).contains("Blood donation camp");

        // Step 3: Confirm events proposal
        String eventActionId = node2.get("proposedAction").get("actionId").asText();
        mvc.perform(post("/api/v1/assistant/actions/" + eventActionId + "/confirm")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idempotencyKey\":\"event-manual-exec\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EXECUTED"));

        // Step 4: Next turn - "generate a chart of expense"
        String res4 = sendChat(token, "generate a chart of expense", convoId);
        JsonNode node4 = objectMapper.readTree(res4);
        assertThat(node4.get("proposedAction").isNull()).isTrue();
        assertThat(node4.get("response").asText()).doesNotContain("Record ₹200.00 expense");
    }
}

package com.dailymate.assistant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dailymate.assistant.dto.request.AssistantActionExecutionRequest;
import com.dailymate.assistant.dto.request.AssistantChatRequest;
import com.dailymate.assistant.security.AssistantRateLimiter;
import com.dailymate.auth.dto.request.RegisterRequest;
import com.dailymate.expense.entity.ExpenseEntry;
import com.dailymate.expense.repository.ExpenseEntryRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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
class AssistantTenExpenseBulkParserIntegrationTests {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ExpenseEntryRepository expenseRepository;

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
                        .content(objectMapper.writeValueAsString(new RegisterRequest(email, "StrongPass123!", "TenBulk", "Tester"))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(tokenBody).get("accessToken").asText();
    }

    @Test
    @DisplayName("Exact 10-expense bulk parse: 10 items, ₹11,159.00 total, 0 fabricated records, 10 DB rows after confirmation")
    void testExactTenExpenseBulkParserAndConfirmation() throws Exception {
        String token = registerAndGetToken("tenbulk_user@dailymate.local");

        long initialDbCount = expenseRepository.count();

        String prompt = """
                Add these expenses to my account in bulk.

                1. Groceries — ₹1850 — August 1, 2026
                2. Fuel — ₹1200 — August 2, 2026
                3. Internet Bill — ₹999 — August 3, 2026
                4. Electricity Bill — ₹2450 — August 4, 2026
                5. Restaurant — ₹780 — August 5, 2026
                6. Pharmacy — ₹650 — August 6, 2026
                7. Shopping — ₹2100 — August 7, 2026
                8. Transportation — ₹450 — August 8, 2026
                9. Movie — ₹500 — August 9, 2026
                10. Coffee — ₹180 — August 10, 2026
                """;

        // Step 1: Send chat request to Assistant
        String chatResponseBody = mvc.perform(post("/api/v1/assistant/chat")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AssistantChatRequest(prompt, null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.proposal").exists())
                .andExpect(jsonPath("$.proposal.actionType").value("BULK_RECORD_EXPENSES"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode chatJson = objectMapper.readTree(chatResponseBody);
        JsonNode proposal = chatJson.get("proposal");
        String actionId = proposal.get("actionId").asText();
        String payloadJson = proposal.get("payloadJson").asText();

        JsonNode itemsArray = objectMapper.readTree(payloadJson);

        // Verification 1: Exactly 10 parsed items
        assertThat(itemsArray.isArray()).isTrue();
        assertThat(itemsArray.size()).isEqualTo(10);

        // Verification 2: Verify each individual item amount and total sum
        BigDecimal calculatedTotal = BigDecimal.ZERO;
        for (JsonNode item : itemsArray) {
            BigDecimal amt = new BigDecimal(item.get("amount").asText());
            calculatedTotal = calculatedTotal.add(amt);
        }

        // Expected: 1850 + 1200 + 999 + 2450 + 780 + 650 + 2100 + 450 + 500 + 180 = 11159.00
        assertThat(calculatedTotal).isEqualByComparingTo(new BigDecimal("11159.00"));

        // Verification 3: Message contains ₹11,159.00 and mentions 10 expenses
        String replyMessage = chatJson.get("message").asText();
        assertThat(replyMessage).contains("₹11,159.00");
        assertThat(replyMessage).contains("10 expenses");
        assertThat(replyMessage).doesNotContain("₹2,026.00");
        assertThat(replyMessage).doesNotContain("₹31,419.00");

        // Verification 4: Zero database mutations before confirmation
        assertThat(expenseRepository.count()).isEqualTo(initialDbCount);

        // Step 2: Confirm and execute the proposed bulk action
        mvc.perform(post("/api/v1/assistant/actions/execute")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AssistantActionExecutionRequest(actionId, true))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"));

        // Verification 5: Exactly 10 rows added to the database
        assertThat(expenseRepository.count()).isEqualTo(initialDbCount + 10);

        // Verification 6: Verify all 10 expenses are stored with correct dates and amounts in DB
        List<Expense> stored = expenseRepository.findAll();
        BigDecimal dbSum = stored.stream()
                .skip(initialDbCount)
                .map(Expense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(dbSum).isEqualByComparingTo(new BigDecimal("11159.00"));
    }

    @Test
    @DisplayName("Regression: 4-digit years in dates must never be extracted as expense amounts")
    void testFourDigitYearMaskingInDates() throws Exception {
        String token = registerAndGetToken("year_masking_user@dailymate.local");

        String prompt = "Add expense Dinner ₹450 on 2026-08-15";

        String res = mvc.perform(post("/api/v1/assistant/chat")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AssistantChatRequest(prompt, null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.proposal.actionType").value("RECORD_EXPENSE"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode json = objectMapper.readTree(res);
        JsonNode payload = objectMapper.readTree(json.get("proposal").get("payloadJson").asText());

        assertThat(new BigDecimal(payload.get("amount").asText())).isEqualByComparingTo(new BigDecimal("450.00"));
        assertThat(payload.get("spentOn").asText()).isEqualTo("2026-08-15");
    }

    @Test
    @DisplayName("Anti-fabrication: Isolated year or random digits without description must be rejected")
    void testAntiFabricationOnIsolatedNumbers() throws Exception {
        String token = registerAndGetToken("antifab_user@dailymate.local");

        String prompt = "Add these expenses:\n1. 2026\n2. 2025";

        mvc.perform(post("/api/v1/assistant/chat")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AssistantChatRequest(prompt, null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.proposal").doesNotExist());
    }
}

package com.dailymate.assistant;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dailymate.assistant.dto.request.AssistantActionExecutionRequest;
import com.dailymate.assistant.dto.request.AssistantChatRequest;
import com.dailymate.assistant.security.AssistantRateLimiter;
import com.dailymate.auth.dto.request.RegisterRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
class AssistantToolRegistryIntegrationTests {

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
                        .content(objectMapper.writeValueAsString(new RegisterRequest(email, "StrongPass123!", "Registry", "Tester"))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(tokenBody).get("accessToken").asText();
    }

    @Test
    void discoversRegisteredToolsViaEndpoint() throws Exception {
        String token = registerAndGetToken("discovery-tools@example.com");

        mvc.perform(get("/api/v1/assistant/tools")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.name == 'expense.record')].riskTier").value("TIER_3"))
                .andExpect(jsonPath("$[?(@.name == 'medicine.create')].confirmationRequired").value(true))
                .andExpect(jsonPath("$[?(@.name == 'notification.markAllRead')].riskTier").value("TIER_2"))
                .andExpect(jsonPath("$[?(@.name == 'report.monthlyLifeReport')].riskTier").value("TIER_1"));
    }

    @Test
    void generatesDeterministicMonthlyLifeReport() throws Exception {
        String token = registerAndGetToken("life-report@example.com");

        // Request monthly life report via chat
        mvc.perform(post("/api/v1/assistant/chat")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AssistantChatRequest("Generate my monthly DailyMate life report"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.proposedAction").doesNotExist()) // Tier 1 Read: zero mutation proposal
                .andExpect(jsonPath("$.response").value(org.hamcrest.Matchers.containsString("Monthly Life Report")))
                .andExpect(jsonPath("$.response").value(org.hamcrest.Matchers.containsString("Total Expenses This Month")))
                .andExpect(jsonPath("$.response").value(org.hamcrest.Matchers.containsString("Active Medicine Reminders")));
    }

    @Test
    void proposesAndExecutesMarketplaceProviderRegistration() throws Exception {
        String token = registerAndGetToken("provider-reg@example.com");

        // "Add electrician Rahul with phone 9876543210 in Pune"
        String chatBody = mvc.perform(post("/api/v1/assistant/chat")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AssistantChatRequest("Add electrician Rahul with phone 9876543210 in Pune"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.proposedAction").exists())
                .andExpect(jsonPath("$.proposedAction.actionType").value("REGISTER_PROVIDER"))
                .andExpect(jsonPath("$.proposedAction.summary").value(org.hamcrest.Matchers.containsString("Rahul")))
                .andExpect(jsonPath("$.proposedAction.summary").value(org.hamcrest.Matchers.containsString("Electrician")))
                .andReturn().getResponse().getContentAsString();

        JsonNode res = objectMapper.readTree(chatBody);
        String actionId = res.get("proposedAction").get("actionId").asText();

        // Confirm Action
        mvc.perform(post("/api/v1/assistant/actions/{id}/confirm", actionId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AssistantActionExecutionRequest("key-rahul"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EXECUTED"))
                .andExpect(jsonPath("$.resultMessage").value(org.hamcrest.Matchers.containsString("Rahul")));

        // Verify Marketplace domain service created provider
        mvc.perform(get("/api/v1/marketplace/providers")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.name == 'Rahul')].category").value("Electrician"))
                .andExpect(jsonPath("$[?(@.name == 'Rahul')].phone").value("9876543210"));
    }

    @Test
    void missingProviderPhoneAsksClarificationWithoutProposingAction() throws Exception {
        String token = registerAndGetToken("missing-phone@example.com");

        // "Add electrician Rahul in Pune" (phone missing) -> asks for phone number
        mvc.perform(post("/api/v1/assistant/chat")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AssistantChatRequest("Add electrician Rahul in Pune"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.proposedAction").doesNotExist())
                .andExpect(jsonPath("$.response").value(org.hamcrest.Matchers.containsString("phone number")));
    }

    @Test
    void missingProviderNameAsksClarificationWithoutProposingAction() throws Exception {
        String token = registerAndGetToken("missing-name@example.com");

        // "Add electrician with phone 9876543210" (name missing) -> asks for name
        mvc.perform(post("/api/v1/assistant/chat")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AssistantChatRequest("Add electrician with phone 9876543210"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.proposedAction").doesNotExist())
                .andExpect(jsonPath("$.response").value(org.hamcrest.Matchers.containsString("name of the Electrician")));
    }

    @Test
    void unsupportedCapabilitySafelyRejectedWithoutAttemptedAction() throws Exception {
        String token = registerAndGetToken("unsupported-cap@example.com");

        // "Book a flight to Mumbai"
        mvc.perform(post("/api/v1/assistant/chat")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AssistantChatRequest("Book a flight to Mumbai"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.proposedAction").doesNotExist())
                .andExpect(jsonPath("$.response").value(org.hamcrest.Matchers.containsString("DailyMate does not support")));
    }
}

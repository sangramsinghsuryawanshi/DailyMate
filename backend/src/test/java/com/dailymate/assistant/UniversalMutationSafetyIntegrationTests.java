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
import com.dailymate.assistant.tool.AssistantToolRegistry;
import com.dailymate.assistant.tool.ToolOperationType;
import com.dailymate.assistant.tool.ToolRiskTier;
import com.dailymate.auth.dto.request.RegisterRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
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
class UniversalMutationSafetyIntegrationTests {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private AssistantToolRegistry toolRegistry;

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
                        .content(objectMapper.writeValueAsString(new RegisterRequest(email, "StrongPass123!", "Universal", "Tester"))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(tokenBody).get("accessToken").asText();
    }

    @Test
    void invariant1_missingRequiredParametersNeverMutateState() throws Exception {
        String token = registerAndGetToken("missing-params@example.com");

        // 1. Missing amount in expense
        mvc.perform(post("/api/v1/assistant/chat")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AssistantChatRequest("Add expense for lunch"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.proposedAction").doesNotExist())
                .andExpect(jsonPath("$.response").value(org.hamcrest.Matchers.containsString("What amount")));

        // 2. Missing blood group in blood request
        mvc.perform(post("/api/v1/assistant/chat")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AssistantChatRequest("Need blood at City Hospital"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.proposedAction").doesNotExist())
                .andExpect(jsonPath("$.response").value(org.hamcrest.Matchers.containsString("blood group is needed")));

        // 3. Missing provider phone
        mvc.perform(post("/api/v1/assistant/chat")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AssistantChatRequest("Add plumber Suresh in Pune"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.proposedAction").doesNotExist())
                .andExpect(jsonPath("$.response").value(org.hamcrest.Matchers.containsString("contact phone number")));

        // 4. Missing emergency contact phone
        mvc.perform(post("/api/v1/assistant/chat")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AssistantChatRequest("Add my brother Ramesh as emergency contact"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.proposedAction").doesNotExist())
                .andExpect(jsonPath("$.response").value(org.hamcrest.Matchers.containsString("phone number")));
    }

    @Test
    void invariant2_unauthorizedRoleIsDeniedServerSide() {
        // Assert registry denies GUEST role across all mutation tools
        toolRegistry.getAllTools().values().stream()
                .filter(t -> t.operationType() == ToolOperationType.MUTATION)
                .forEach(t -> {
                    org.junit.jupiter.api.Assertions.assertThrows(
                            com.dailymate.core.exception.ForbiddenException.class,
                            () -> toolRegistry.validateAuthorization(t.name(), "GUEST")
                    );
                });
    }

    @Test
    void invariant3_crossTenantActionConfirmationDeniedWith404() throws Exception {
        String tokenA = registerAndGetToken("user-a-cross@example.com");
        String tokenB = registerAndGetToken("user-b-cross@example.com");

        // User A proposes action
        String chatBody = mvc.perform(post("/api/v1/assistant/chat")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AssistantChatRequest("Add expense 450 for Health"))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String actionId = objectMapper.readTree(chatBody).get("proposedAction").get("actionId").asText();

        // User B attempts to confirm User A's action -> 404
        mvc.perform(post("/api/v1/assistant/actions/{id}/confirm", actionId)
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound());
    }

    @Test
    void invariant4_tier3ToolsRequireConfirmationBeforeDomainExecution() throws Exception {
        String token = registerAndGetToken("tier3-confirm@example.com");

        // Proposal creation
        String chatBody = mvc.perform(post("/api/v1/assistant/chat")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AssistantChatRequest("Add expense 300 for Utilities"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.proposedAction").exists())
                .andExpect(jsonPath("$.proposedAction.status").value("PENDING"))
                .andReturn().getResponse().getContentAsString();

        // Verify zero expense records exist prior to confirmation
        mvc.perform(get("/api/v1/expenses")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void invariant5_idempotencyReplayProducesZeroDuplicateMutations() throws Exception {
        String token = registerAndGetToken("idemp-universal@example.com");

        String chatBody = mvc.perform(post("/api/v1/assistant/chat")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AssistantChatRequest("Add expense 250 for Groceries"))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String actionId = objectMapper.readTree(chatBody).get("proposedAction").get("actionId").asText();

        // Confirm 1
        mvc.perform(post("/api/v1/assistant/actions/{id}/confirm", actionId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AssistantActionExecutionRequest("key-universal-1"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EXECUTED"));

        // Replay Confirm 2 (identical key)
        mvc.perform(post("/api/v1/assistant/actions/{id}/confirm", actionId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AssistantActionExecutionRequest("key-universal-1"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EXECUTED"));

        // Exactly 1 expense record in DB
        mvc.perform(get("/api/v1/expenses")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void invariant6_failureTruthfulnessAndZeroFabricatedSuccess() throws Exception {
        String token = registerAndGetToken("failure-truth@example.com");

        // Request execution with non-existent action ID
        String nonExistentActionId = UUID.randomUUID().toString();

        mvc.perform(post("/api/v1/assistant/actions/{id}/confirm", nonExistentActionId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }
}

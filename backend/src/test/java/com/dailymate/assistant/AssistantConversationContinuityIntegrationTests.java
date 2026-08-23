package com.dailymate.assistant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dailymate.assistant.dto.request.AssistantActionExecutionRequest;
import com.dailymate.assistant.dto.request.AssistantChatRequest;
import com.dailymate.auth.dto.request.RegisterRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
class AssistantConversationContinuityIntegrationTests {

    @Autowired
    private MockMvc mvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private String registerAndGetToken(String email) throws Exception {
        RegisterRequest req = new RegisterRequest(email, "StrongPass123!", "Test", "User");
        String tokenBody = mvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(tokenBody).get("accessToken").asText();
    }

    @Test
    void multiTurnCorrection_updatesProposalWithoutAutomaticExecution() throws Exception {
        String token = registerAndGetToken("continuity-user@example.com");

        // Turn 1: Add ₹50 for khichadi
        String chatBody1 = mvc.perform(post("/api/v1/assistant/chat")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AssistantChatRequest("Add expense for my afternoon lunch name khichadi and amount is 50"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.proposedAction.actionType").value("RECORD_EXPENSE"))
                .andExpect(jsonPath("$.proposedAction.summary").value("Record ₹50.00 expense for Khichadi"))
                .andReturn().getResponse().getContentAsString();

        JsonNode chat1Node = objectMapper.readTree(chatBody1);
        String conversationId = chat1Node.get("id").asText();
        String proposal1Id = chat1Node.get("proposedAction").get("actionId").asText();

        // Turn 2: "Actually make that 60"
        AssistantChatRequest req2 = new AssistantChatRequest("Actually make that 60", conversationId);
        String chatBody2 = mvc.perform(post("/api/v1/assistant/chat")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req2)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.proposedAction.actionType").value("RECORD_EXPENSE"))
                .andExpect(jsonPath("$.proposedAction.summary").value("Record ₹60.00 expense for Khichadi"))
                .andExpect(jsonPath("$.response").value(org.hamcrest.Matchers.containsString("₹60.00")))
                .andReturn().getResponse().getContentAsString();

        JsonNode chat2Node = objectMapper.readTree(chatBody2);
        String proposal2Id = chat2Node.get("proposedAction").get("actionId").asText();

        // Attempt to confirm old proposal 1 -> Must be rejected (SUPERSEDED)
        mvc.perform(post("/api/v1/assistant/actions/" + proposal1Id + "/confirm")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AssistantActionExecutionRequest("Confirmed"))))
                .andExpect(status().isConflict());

        // Confirm proposal 2 -> Success
        mvc.perform(post("/api/v1/assistant/actions/" + proposal2Id + "/confirm")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AssistantActionExecutionRequest("Confirmed"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EXECUTED"));
    }

    @Test
    void multiTurnFollowup_resolvesEntityForDeletion() throws Exception {
        String token = registerAndGetToken("followup-delete@example.com");

        // Turn 1: Add ₹120 for Coffee
        String chatBody1 = mvc.perform(post("/api/v1/assistant/chat")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AssistantChatRequest("Add expense 120 for Coffee"))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        JsonNode chat1Node = objectMapper.readTree(chatBody1);
        String conversationId = chat1Node.get("id").asText();
        String proposalId = chat1Node.get("proposedAction").get("actionId").asText();

        // Execute action
        mvc.perform(post("/api/v1/assistant/actions/" + proposalId + "/confirm")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AssistantActionExecutionRequest("Confirmed"))))
                .andExpect(status().isOk());

        // Turn 2: "Delete the one I just added"
        AssistantChatRequest req2 = new AssistantChatRequest("Delete the one I just added", conversationId);
        mvc.perform(post("/api/v1/assistant/chat")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req2)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.proposedAction.actionType").value("DELETE_EXPENSE"));
    }

    @Test
    void tenantIsolation_userBCannotAccessUserAConversation() throws Exception {
        String tokenA = registerAndGetToken("user-a-tenant@example.com");
        String tokenB = registerAndGetToken("user-b-tenant@example.com");

        // User A starts conversation
        String chatBodyA = mvc.perform(post("/api/v1/assistant/chat")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AssistantChatRequest("Add expense 75 for lunch"))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String conversationIdA = objectMapper.readTree(chatBodyA).get("id").asText();

        // User B attempts to access / delete User A's conversation
        mvc.perform(get("/api/v1/assistant/conversations")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == '" + conversationIdA + "')]").doesNotExist());
    }
}

package com.dailymate.assistant;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
class AssistantControllerIntegrationTests {

    @Autowired
    private MockMvc mvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void createsListsUpdatesAndDeletesAssistantConversations() throws Exception {
        RegisterRequest registerRequest = new RegisterRequest("assistant@example.com", "StrongPass123!", "User", "Assistant");
        String registerJson = objectMapper.writeValueAsString(registerRequest);

        String tokenBody = mvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String token = objectMapper.readTree(tokenBody).get("accessToken").asText();

        String conversationBody = mvc.perform(post("/api/v1/assistant/conversations")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Daily plan\",\"prompt\":\"Plan my week\",\"response\":\"Focus on health, work, and family tasks\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Daily plan"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode conversation = objectMapper.readTree(conversationBody);
        String conversationId = conversation.get("id").asText();

        mvc.perform(get("/api/v1/assistant/conversations")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Daily plan"));

        mvc.perform(patch("/api/v1/assistant/conversations/{id}", conversationId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Daily plan updated\",\"prompt\":\"Plan my week with exercise and errands\",\"response\":\"Exercise, errands, and rest blocks for a balanced week\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Daily plan updated"));

        mvc.perform(delete("/api/v1/assistant/conversations/{id}", conversationId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
    }
}

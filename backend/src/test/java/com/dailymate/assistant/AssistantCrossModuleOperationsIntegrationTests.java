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
class AssistantCrossModuleOperationsIntegrationTests {

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
                        .content(objectMapper.writeValueAsString(new RegisterRequest(email, "StrongPass123!", "Cross", "Tester"))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(tokenBody).get("accessToken").asText();
    }

    @Test
    void proposesAndExecutesEmergencyBloodRequest() throws Exception {
        String token = registerAndGetToken("blood-ops@example.com");

        // "Need 2 units of O+ blood for Rahul at Ruby Hall Clinic Pune, call 9876543210"
        String chatBody = mvc.perform(post("/api/v1/assistant/chat")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AssistantChatRequest("Need 2 units of O+ blood for Rahul at Ruby Hall Clinic Pune, call 9876543210"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.proposedAction").exists())
                .andExpect(jsonPath("$.proposedAction.actionType").value("CREATE_BLOOD_REQUEST"))
                .andExpect(jsonPath("$.proposedAction.summary").value(org.hamcrest.Matchers.containsString("O+")))
                .andExpect(jsonPath("$.proposedAction.summary").value(org.hamcrest.Matchers.containsString("Rahul")))
                .andReturn().getResponse().getContentAsString();

        JsonNode res = objectMapper.readTree(chatBody);
        String actionId = res.get("proposedAction").get("actionId").asText();

        // Confirm
        mvc.perform(post("/api/v1/assistant/actions/{id}/confirm", actionId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AssistantActionExecutionRequest("key-blood-1"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EXECUTED"))
                .andExpect(jsonPath("$.resultMessage").value(org.hamcrest.Matchers.containsString("Rahul")));

        // Verify Blood Request exists in Blood module
        mvc.perform(get("/api/v1/blood/requests")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.patientName == 'Rahul')].bloodGroup").value("O+"))
                .andExpect(jsonPath("$[?(@.patientName == 'Rahul')].unitsNeeded").value(2));
    }

    @Test
    void proposesAndExecutesPersonalIceContactAddition() throws Exception {
        String token = registerAndGetToken("ice-ops@example.com");

        // "Add my wife Priya with phone 9876543210 as emergency contact"
        String chatBody = mvc.perform(post("/api/v1/assistant/chat")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AssistantChatRequest("Add my wife Priya with phone 9876543210 as emergency contact"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.proposedAction").exists())
                .andExpect(jsonPath("$.proposedAction.actionType").value("CREATE_ICE_CONTACT"))
                .andExpect(jsonPath("$.proposedAction.summary").value(org.hamcrest.Matchers.containsString("Priya")))
                .andExpect(jsonPath("$.proposedAction.summary").value(org.hamcrest.Matchers.containsString("Wife")))
                .andReturn().getResponse().getContentAsString();

        JsonNode res = objectMapper.readTree(chatBody);
        String actionId = res.get("proposedAction").get("actionId").asText();

        // Confirm
        mvc.perform(post("/api/v1/assistant/actions/{id}/confirm", actionId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AssistantActionExecutionRequest("key-ice-1"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EXECUTED"))
                .andExpect(jsonPath("$.resultMessage").value(org.hamcrest.Matchers.containsString("Priya")));

        // Verify personal ICE Contact exists in Emergency module
        mvc.perform(get("/api/v1/emergency-contacts/my-contacts")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.name == 'Priya')].phone").value("9876543210"));
    }
}

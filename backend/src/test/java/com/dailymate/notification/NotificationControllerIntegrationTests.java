package com.dailymate.notification;

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
class NotificationControllerIntegrationTests {

    @Autowired
    private MockMvc mvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void createsListsUpdatesAndDeletesNotifications() throws Exception {
        RegisterRequest registerRequest = new RegisterRequest("notifications@example.com", "StrongPass123!", "User", "Notifications");
        String registerJson = objectMapper.writeValueAsString(registerRequest);

        String tokenBody = mvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String token = objectMapper.readTree(tokenBody).get("accessToken").asText();

        String notificationBody = mvc.perform(post("/api/v1/notifications")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Welcome\",\"message\":\"Your DailyMate profile is ready.\",\"type\":\"info\",\"read\":false}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Welcome"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode notification = objectMapper.readTree(notificationBody);
        String notificationId = notification.get("id").asText();

        mvc.perform(get("/api/v1/notifications")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Welcome"));

        mvc.perform(patch("/api/v1/notifications/{id}", notificationId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Welcome back\",\"message\":\"Your DailyMate profile is ready and synced.\",\"type\":\"success\",\"read\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Welcome back"))
                .andExpect(jsonPath("$.read").value(true));

        mvc.perform(delete("/api/v1/notifications/{id}", notificationId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
    }
}

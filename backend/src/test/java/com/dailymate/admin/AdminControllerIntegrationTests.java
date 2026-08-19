package com.dailymate.admin;

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
class AdminControllerIntegrationTests {

    @Autowired
    private MockMvc mvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void listsComplaintsAndAllowsStatusUpdates() throws Exception {
        String complaintBody = mvc.perform(post("/api/v1/community-complaints/complaints")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Broken bus stop\",\"category\":\"Transport\",\"location\":\"Main Street\",\"description\":\"The shelter is damaged and unsafe\"}"))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String tokenBody = mvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RegisterRequest("admin@example.com", "StrongPass123!", "Admin", "User"))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String token = objectMapper.readTree(tokenBody).get("accessToken").asText();
        JsonNode complaint = objectMapper.readTree(complaintBody);
        String complaintId = complaint.get("id").asText();

        String listBody = mvc.perform(get("/api/v1/admin/complaints")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode complaints = objectMapper.readTree(listBody);
        boolean containsComplaint = false;
        for (JsonNode complaintItem : complaints) {
            if ("Broken bus stop".equals(complaintItem.get("title").asText())) {
                containsComplaint = true;
                break;
            }
        }
        org.junit.jupiter.api.Assertions.assertTrue(containsComplaint);

        mvc.perform(patch("/api/v1/admin/complaints/{id}/status", complaintId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"IN_REVIEW\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_REVIEW"));
    }
}

package com.dailymate.community;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
class CommunityComplaintControllerIntegrationTests {

    @Autowired
    private MockMvc mvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void createsListsUpdatesAndDeletesCommunityComplaints() throws Exception {
        String complaintBody = mvc.perform(post("/api/v1/community-complaints/complaints")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Broken street light\",\"category\":\"Infrastructure\",\"location\":\"Oak Avenue\",\"description\":\"Street light flickers at night and needs repair\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Broken street light"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode complaint = objectMapper.readTree(complaintBody);
        String complaintId = complaint.get("id").asText();

        String listBody = mvc.perform(get("/api/v1/community-complaints/complaints"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode complaints = objectMapper.readTree(listBody);
        assertTrue(complaints.elements().hasNext());
        boolean containsComplaint = false;
        for (JsonNode complaintItem : complaints) {
            if ("Broken street light".equals(complaintItem.get("title").asText())) {
                containsComplaint = true;
                break;
            }
        }
        assertTrue(containsComplaint);

        mvc.perform(patch("/api/v1/community-complaints/complaints/{id}", complaintId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Broken street light fixed\",\"category\":\"Infrastructure\",\"location\":\"Oak Avenue\",\"description\":\"Light remains dim and needs a full replacement\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Broken street light fixed"));

        mvc.perform(delete("/api/v1/community-complaints/complaints/{id}", complaintId))
                .andExpect(status().isNoContent());
    }
}

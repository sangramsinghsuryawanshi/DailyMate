package com.dailymate.community;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dailymate.auth.dto.request.RegisterRequest;
import com.dailymate.community.dto.request.CommunityComplaintRequest;
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

    private String registerAndGetToken(String email) throws Exception {
        String tokenBody = mvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RegisterRequest(email, "StrongPass123!", "Community", "User"))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(tokenBody).get("accessToken").asText();
    }

    @Test
    void publicCanViewComplaintsButCannotMutateWithoutAuth() throws Exception {
        // Public GET -> 200 OK
        mvc.perform(get("/api/v1/community-complaints/complaints"))
                .andExpect(status().isOk());

        CommunityComplaintRequest request = new CommunityComplaintRequest(
                "Noise complaint", "Noise", "Park View", "Loud music at late hours");

        // Anonymous POST -> 401 Unauthorized
        mvc.perform(post("/api/v1/community-complaints/complaints")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());

        // Anonymous PATCH -> 401 Unauthorized
        mvc.perform(patch("/api/v1/community-complaints/complaints/fake-id")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());

        // Anonymous DELETE -> 401 Unauthorized
        mvc.perform(delete("/api/v1/community-complaints/complaints/fake-id"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createsListsUpdatesAndDeletesCommunityComplaints() throws Exception {
        String token = registerAndGetToken("community-test@example.com");

        CommunityComplaintRequest request = new CommunityComplaintRequest(
                "Broken street light", "Infrastructure", "Oak Avenue", "Street light flickers at night and needs repair");

        String complaintBody = mvc.perform(post("/api/v1/community-complaints/complaints")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Broken street light"))
                .andExpect(jsonPath("$.status").value("OPEN"))
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

        CommunityComplaintRequest updateRequest = new CommunityComplaintRequest(
                "Broken street light fixed", "Infrastructure", "Oak Avenue", "Light remains dim and needs a full replacement");

        mvc.perform(patch("/api/v1/community-complaints/complaints/{id}", complaintId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Broken street light fixed"));

        mvc.perform(delete("/api/v1/community-complaints/complaints/{id}", complaintId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
    }

    @Test
    void rejectsInvalidComplaintPayloadWith400() throws Exception {
        String token = registerAndGetToken("invalid-complaint@example.com");

        CommunityComplaintRequest invalid = new CommunityComplaintRequest("", "", "", "");

        mvc.perform(post("/api/v1/community-complaints/complaints")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.title").exists())
                .andExpect(jsonPath("$.errors.category").exists())
                .andExpect(jsonPath("$.errors.location").exists())
                .andExpect(jsonPath("$.errors.description").exists());
    }
}

package com.dailymate.marketplace;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dailymate.auth.dto.request.RegisterRequest;
import com.dailymate.marketplace.dto.request.ServiceProviderRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
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
class MarketplaceControllerIntegrationTests {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String registerAndGetToken(String email) throws Exception {
        String tokenBody = mvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RegisterRequest(email, "StrongPass123!", "Market", "User"))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(tokenBody).get("accessToken").asText();
    }

    @Test
    void anonymousCanDiscoverProvidersAndFetchDetails() throws Exception {
        mvc.perform(get("/api/v1/marketplace/providers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].category").value("Electrician"));

        String response = mvc.perform(get("/api/v1/marketplace/providers"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode providers = objectMapper.readTree(response);
        String providerId = providers.get(0).get("id").asText();

        mvc.perform(get("/api/v1/marketplace/providers/{id}", providerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(providerId));
    }

    @Test
    void anonymousCannotCreateProvider() throws Exception {
        ServiceProviderRequest request = new ServiceProviderRequest(
                "Pro Cleaners", "Cleaner", "Home cleaning", "Downtown", "+1-555-9999", "pro@example.com", new BigDecimal("45.00"));

        mvc.perform(post("/api/v1/marketplace/providers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsInvalidCreationPayload() throws Exception {
        String token = registerAndGetToken("invalid-payload@example.com");

        ServiceProviderRequest invalidRequest = new ServiceProviderRequest(
                "", "", "", "", "+1-555-9999", "not-an-email", new BigDecimal("-10.00"));

        mvc.perform(post("/api/v1/marketplace/providers")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").exists());
    }

    @Test
    void ownerCanCreateUpdateAndDeleteProviderWhileNonOwnerIsForbidden() throws Exception {
        String ownerToken = registerAndGetToken("owner@example.com");
        String nonOwnerToken = registerAndGetToken("nonowner@example.com");

        // 1. Owner creates provider
        ServiceProviderRequest createRequest = new ServiceProviderRequest(
                "Elite Plumbing", "Plumber", "Emergency leak repairs and pipe fittings",
                "North District", "+1-555-1234", "elite@example.com", new BigDecimal("75.00"));

        String createdBody = mvc.perform(post("/api/v1/marketplace/providers")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Elite Plumbing"))
                .andExpect(jsonPath("$.hourlyRate").value(75.00))
                .andExpect(jsonPath("$.userId").isNotEmpty())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String providerId = objectMapper.readTree(createdBody).get("id").asText();

        // 2. Non-owner attempts to update -> 403 Forbidden
        ServiceProviderRequest updateRequest = new ServiceProviderRequest(
                "Hacked Plumbing", "Plumber", "Unauthorized update",
                "South District", "+1-555-0000", "hack@example.com", new BigDecimal("10.00"));

        mvc.perform(patch("/api/v1/marketplace/providers/{id}", providerId)
                        .header("Authorization", "Bearer " + nonOwnerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isForbidden());

        // 3. Owner updates successfully -> 200 OK
        ServiceProviderRequest ownerUpdateRequest = new ServiceProviderRequest(
                "Elite Plumbing Pro", "Plumber", "Emergency leak repairs, pipe fittings, and drain cleaning",
                "North & Central Districts", "+1-555-1234", "pro@elite.example.com", new BigDecimal("85.50"));

        mvc.perform(patch("/api/v1/marketplace/providers/{id}", providerId)
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ownerUpdateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Elite Plumbing Pro"))
                .andExpect(jsonPath("$.hourlyRate").value(85.50))
                .andExpect(jsonPath("$.serviceArea").value("North & Central Districts"));

        // 4. Non-owner attempts to delete -> 403 Forbidden
        mvc.perform(delete("/api/v1/marketplace/providers/{id}", providerId)
                        .header("Authorization", "Bearer " + nonOwnerToken))
                .andExpect(status().isForbidden());

        // 5. Owner deletes successfully -> 204 No Content
        mvc.perform(delete("/api/v1/marketplace/providers/{id}", providerId)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isNoContent());

        // 6. Deleted provider is no longer found
        mvc.perform(get("/api/v1/marketplace/providers/{id}", providerId))
                .andExpect(status().isNotFound());
    }
}

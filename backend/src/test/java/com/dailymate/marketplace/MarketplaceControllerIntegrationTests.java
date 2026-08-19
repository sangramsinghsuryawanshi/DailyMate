package com.dailymate.marketplace;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
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

    @Test
    void listsServiceProviders() throws Exception {
        mvc.perform(get("/api/v1/marketplace/providers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].category").value("Electrician"));
    }

    @Test
    void fetchesServiceProviderDetails() throws Exception {
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
}

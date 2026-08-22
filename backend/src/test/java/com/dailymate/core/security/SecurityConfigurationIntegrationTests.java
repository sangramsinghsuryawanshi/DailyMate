package com.dailymate.core.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityConfigurationIntegrationTests {

    @Autowired
    private MockMvc mvc;

    @Test
    void anonymousAssistantRequestReturns401() throws Exception {
        mvc.perform(get("/api/v1/assistant/conversations"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void anonymousLostFoundMyPostsRequestReturns401() throws Exception {
        mvc.perform(get("/api/v1/lost-found/my-posts"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void anonymousPublicMarketplaceProvidersReturns200() throws Exception {
        mvc.perform(get("/api/v1/marketplace/providers"))
                .andExpect(status().isOk());
    }
}

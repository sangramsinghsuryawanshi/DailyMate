package com.dailymate.events;

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
class LocalEventControllerIntegrationTests {

    @Autowired
    private MockMvc mvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void createsListsUpdatesAndDeletesLocalEvents() throws Exception {
        String eventBody = mvc.perform(post("/api/v1/events/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Community Cleanup\",\"category\":\"Volunteer\",\"location\":\"Riverside Park\",\"eventDate\":\"2026-09-12T10:00:00Z\",\"description\":\"Join residents for a neighborhood cleanup drive\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Community Cleanup"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode event = objectMapper.readTree(eventBody);
        String eventId = event.get("id").asText();

        mvc.perform(get("/api/v1/events/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Community Cleanup"));

        mvc.perform(patch("/api/v1/events/events/{id}", eventId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Community Cleanup Day\",\"category\":\"Volunteer\",\"location\":\"Lakeside Park\",\"eventDate\":\"2026-09-20T09:30:00Z\",\"description\":\"A larger volunteer neighborhood cleanup weekend\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Community Cleanup Day"));

        mvc.perform(delete("/api/v1/events/events/{id}", eventId))
                .andExpect(status().isNoContent());
    }
}

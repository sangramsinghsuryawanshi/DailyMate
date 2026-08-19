package com.dailymate.blood;

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
class BloodDonationControllerIntegrationTests {

    @Autowired
    private MockMvc mvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void createsListsUpdatesAndDeletesDonationCenters() throws Exception {
        String centerBody = mvc.perform(post("/api/v1/blood/centers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"City Blood Bank\",\"location\":\"Downtown Plaza\",\"contact\":\"+1-555-0111\",\"description\":\"24-hour donation support\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("City Blood Bank"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode center = objectMapper.readTree(centerBody);
        String centerId = center.get("id").asText();

        mvc.perform(get("/api/v1/blood/centers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("City Blood Bank"));

        mvc.perform(patch("/api/v1/blood/centers/{id}", centerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"City Blood Bank West\",\"location\":\"West End\",\"contact\":\"+1-555-0222\",\"description\":\"Weekend donation support\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("City Blood Bank West"));

        mvc.perform(delete("/api/v1/blood/centers/{id}", centerId))
                .andExpect(status().isNoContent());
    }
}

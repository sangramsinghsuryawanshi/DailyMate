package com.dailymate.emergency;

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
class EmergencyContactControllerIntegrationTests {

    @Autowired
    private MockMvc mvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void createsListsUpdatesAndDeletesEmergencyContacts() throws Exception {
        String contactBody = mvc.perform(post("/api/v1/emergency-contacts/contacts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Downtown Clinic\",\"category\":\"Hospital\",\"phone\":\"+1-555-1000\",\"location\":\"Central Plaza\",\"description\":\"24-hour urgent care support\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Downtown Clinic"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode contact = objectMapper.readTree(contactBody);
        String contactId = contact.get("id").asText();

        mvc.perform(get("/api/v1/emergency-contacts/contacts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Downtown Clinic"));

        mvc.perform(patch("/api/v1/emergency-contacts/contacts/{id}", contactId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Harbor Clinic\",\"category\":\"Hospital\",\"phone\":\"+1-555-2000\",\"location\":\"Harbor District\",\"description\":\"Weekend urgent care support\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Harbor Clinic"));

        mvc.perform(delete("/api/v1/emergency-contacts/contacts/{id}", contactId))
                .andExpect(status().isNoContent());
    }
}

package com.dailymate.medicine;

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
class MedicineReminderControllerIntegrationTests {

    @Autowired
    private MockMvc mvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void createsListsUpdatesAndDeletesReminders() throws Exception {
        String registration =
                "{\"email\":\"medicine@example.com\",\"password\":\"correct-horse-battery-staple\",\"firstName\":\"Daily\",\"lastName\":\"Mate\"}";
        String body = mvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registration))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode session = objectMapper.readTree(body);
        String accessToken = session.get("accessToken").asText();

        String reminderBody = mvc.perform(post("/api/v1/medicine-reminders")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Vitamin D\",\"dosage\":\"1000 IU\",\"frequency\":\"Daily\",\"remindAt\":\"08:30\",\"notes\":\"After breakfast\",\"active\":true}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Vitamin D"))
                .andExpect(jsonPath("$.active").value(true))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode reminder = objectMapper.readTree(reminderBody);
        String reminderId = reminder.get("id").asText();

        mvc.perform(get("/api/v1/medicine-reminders")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Vitamin D"));

        mvc.perform(patch("/api/v1/medicine-reminders/{id}", reminderId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Vitamin D\",\"dosage\":\"2000 IU\",\"frequency\":\"Daily\",\"remindAt\":\"09:00\",\"notes\":\"With breakfast\",\"active\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dosage").value("2000 IU"))
                .andExpect(jsonPath("$.active").value(false));

        mvc.perform(delete("/api/v1/medicine-reminders/{id}", reminderId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNoContent());
    }
}
